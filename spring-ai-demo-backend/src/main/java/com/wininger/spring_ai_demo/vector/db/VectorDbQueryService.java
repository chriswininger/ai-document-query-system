package com.wininger.spring_ai_demo.vector.db;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;

import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.util.List;

public class VectorDbQueryService {
  /*public static void main(String args[]) {
    test();
  }*/

  public static void test() {
    // Configure embedding model (equivalent to HuggingFaceEmbeddings)
    try (HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance("sentence-transformers/all-MiniLM-l6-v2")) {
      var query = tokenizer.encode("hello");

      System.out.println("!!! query: " + query);
    }

    // Initialize a SimpleVectorStore instance
    //EmbeddingModel embeddingModel = EmbeddingMode
//    SimpleVectorStore vectorStore = SimpleVectorStore.builder().build();
//
//    // Add vectors to the store
//    float[] vector1 = {0.1f, 0.2f, 0.3f};
//    float[] vector2 = {0.5f, 0.6f, 0.7f};
//    vectorStore.add("vector1", vector1);
//    vectorStore.add("vector2", vector2);
//
//    // Query vector
//    float[] queryVector = {0.12f, 0.22f, 0.28f};
//
//    // Retrieve the most similar vectors
//    List<String> results = vectorStore.findNearest(queryVector, 5);
//
//    // Print results
//    System.out.println("Most similar vectors: " + results);

  }
}

