package com.mongodb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.CreateCollectionOptions;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.*;
import dev.langchain4j.store.embedding.mongodb.IndexMapping;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;
import dev.langchain4j.service.AiServices;
import org.bson.conversions.Bson;
import dev.langchain4j.data.document.splitter.DocumentSplitters;

import java.io.*;
import java.util.*;

public class LangChainRagApp {

    public static void main(String[] args) {
        try {
            // MongoDB setup
            MongoClient mongoClient = MongoClients.create("CONNECTION-URI");

            // Embedding Store
            EmbeddingStore<TextSegment> embeddingStore = createEmbeddingStore(mongoClient);

            // Embedding Model setup
            OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey("OPEN-AI-API-KEY")
                    .modelName(OpenAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002)
                    .build();

            // Chat Model setup
            ChatLanguageModel chatModel = OpenAiChatModel.builder()
                    .apiKey("OPEN-AI-API-KEY")
                    .modelName("gpt-4")
                    .build();

            // Load documents
            String resourcePath = "devcenter-content-snapshot.2024-05-20.json";
            List<TextSegment> documents = loadJsonDocuments(resourcePath, 800, 200);

            System.out.println("Loaded " + documents.size() + " documents");

            for (int i = 0; i < documents.size()/10; i++) {
                TextSegment segment = documents.get(i);
                Embedding embedding = embeddingModel.embed(segment.text()).content();
                embeddingStore.add(embedding, segment);
            }

            System.out.println("Stored embeddings");

            // Content Retriever
            ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .maxResults(5)
                    .minScore(0.75)
                    .build();

            // Assistant
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(chatModel)
                    .contentRetriever(contentRetriever)
                    .build();

            String output = assistant.answer("How to use Atlas Triggers and AI to summarise AirBnB reviews?");

            System.out.println(output);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static EmbeddingStore<TextSegment> createEmbeddingStore(MongoClient mongoClient) {
        String databaseName = "rag_app";
        String collectionName = "embeddings";
        String indexName = "embedding";
        Long maxResultRatio = 10L;
        CreateCollectionOptions createCollectionOptions = new CreateCollectionOptions();
        Bson filter = null;
        Set<String> metadataFields = new HashSet<>();
        IndexMapping indexMapping = new IndexMapping(1536, metadataFields);
        Boolean createIndex = true;

        return new MongoDbEmbeddingStore(
                mongoClient,
                databaseName,
                collectionName,
                indexName,
                maxResultRatio,
                createCollectionOptions,
                filter,
                indexMapping,
                createIndex
        );
    }

    private static List<TextSegment> loadJsonDocuments(String resourcePath, int maxTokensPerChunk, int overlapTokens) throws IOException {
        List<Document> documents = new ArrayList<>();

        // Load file from resources using the ClassLoader
        InputStream inputStream = LangChainRagApp.class.getClassLoader().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new FileNotFoundException("Resource not found: " + resourcePath);
        }

        // Jackson ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        // Read the file line by line
        String line;
        while ((line = reader.readLine()) != null) {
            JsonNode jsonNode = objectMapper.readTree(line);

            String title = jsonNode.path("title").asText(null);
            String body = jsonNode.path("body").asText(null);
            JsonNode metadataNode = jsonNode.path("metadata");

            if (body != null) {
                // Combine title and body for better context
                String text = (title != null ? title + "\n\n" + body : body);

                // Parse metadata
                Metadata metadata = new Metadata();
                if (metadataNode != null && metadataNode.isObject()) {
                    Iterator<String> fieldNames = metadataNode.fieldNames();
                    while (fieldNames.hasNext()) {
                        String fieldName = fieldNames.next();
                        metadata.put(fieldName, metadataNode.path(fieldName).asText());
                    }
                }

                // Create a Document
                Document document = Document.from(text, metadata);
                documents.add(document);
            }
        }

        // Split documents into text segments
        return splitIntoChunks(documents, maxTokensPerChunk, overlapTokens);
    }

    private static List<TextSegment> splitIntoChunks(List<Document> documents, int maxTokensPerChunk, int overlapTokens) {
        // Create a tokenizer for OpenAI
        OpenAiTokenizer tokenizer = new OpenAiTokenizer(OpenAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002);

        // Create a recursive document splitter with the specified token size and overlap
        DocumentSplitter splitter = DocumentSplitters.recursive(
                maxTokensPerChunk,
                overlapTokens,
                tokenizer
        );

        List<TextSegment> allSegments = new ArrayList<>();
        for (Document document : documents) {
            List<TextSegment> segments = splitter.split(document);
            allSegments.addAll(segments);
        }

        return allSegments;
    }

}
