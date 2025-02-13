# Retrieval-Augmented Generation (RAG) with LangChain4J and MongoDB

## Overview
This repository contains a tutorial on building a retrieval-augmented generation (RAG) chatbot using Java, LangChain4J, and MongoDB Atlas. The application enables querying a knowledge base, retrieving relevant documents, and enhancing responses from a large language model (LLM).

## Prerequisites
Ensure you have the following installed:

- Java 21 or higher
- Maven (or Gradle, though this tutorial uses Maven)
- A MongoDB Atlas account with an active cluster
- An OpenAI API key

## Cloning and Running the Application

### 1. Clone the Repository
```sh
git clone https://github.com/YOUR-USERNAME/rag-tutorial.git
cd rag-tutorial
```

### 2. Configure Environment Variables
Create a `.env` file in the root directory and add the following:
```
MONGO_URI=<your-mongodb-connection-string>
OPENAI_API_KEY=<your-openai-api-key>
```
Replace `<your-mongodb-connection-string>` and `<your-openai-api-key>` with your actual credentials.

### 3. Build the Project
Using Maven:
```sh
mvn clean install
```

### 4. Run the Application
```sh
mvn exec:java -Dexec.mainClass="com.mongodb.LangChainRagApp"
```

Alternatively, if using an IDE like IntelliJ IDEA or VS Code, run the `main` method in `LangChainRagApp.java`.

## Dependencies
This project uses the following dependencies:
```xml
<dependencies>  
    <dependency>  
        <groupId>dev.langchain4j</groupId>  
        <artifactId>langchain4j-open-ai</artifactId>  
        <version>1.0.0-alpha1</version>  
    </dependency>  
    <dependency>  
        <groupId>dev.langchain4j</groupId>  
        <artifactId>langchain4j-mongodb-atlas</artifactId>  
        <version>1.0.0-alpha1</version>  
    </dependency>  
    <dependency>  
        <groupId>dev.langchain4j</groupId>  
        <artifactId>langchain4j</artifactId>  
        <version>1.0.0-alpha1</version>  
    </dependency>  
    <dependency>  
        <groupId>com.fasterxml.jackson.core</groupId>  
        <artifactId>jackson-databind</artifactId>  
        <version>2.18.1</version>  
    </dependency>  
</dependencies>
```

## Usage
Once the application is running, you can test it by querying the chatbot with:
```sh
How does Atlas Vector Search work?
```
The chatbot will retrieve relevant documents and generate a response using GPT-4.

## Troubleshooting
- Ensure your MongoDB Atlas cluster is running and accessible.
- Verify that your OpenAI API key is valid.
- Check that you have Java 21 or higher installed by running `java -version`.
- If you encounter dependency issues, try running:
  ```sh
  mvn clean install -U
  ```

## Contributions
Feel free to open an issue or submit a pull request for improvements!

