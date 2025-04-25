package com.wininger.spring_ai_demo.rag;

import org.jooq.meta.derby.sys.Sys;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VectorStorageService {
  private final DocumentImportService documentImportService;
  private final VectorStore vectoreStore;

  public VectorStorageService(
      final DocumentImportService documentImportService,
      final VectorStore vectorStore
  ) {
    this.documentImportService = documentImportService;
    this.vectoreStore = vectorStore;
  }

  // super helpful https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html
  public int storeChunks(final int documentId) {
    System.out.println("!!! processing documentId: " + documentId);
    final var chunks = documentImportService.getAllDocumentChunksByDocId(documentId);

    System.out.println("!!! num chunks: " + chunks.size());
    final List<Document> docsToEmbedAndStore = chunks.stream().map(chunk -> {
      System.out.println("!!! rough token count: " + chunk.content().split("\\s+").length);
      return new Document(chunk.content(), Map.of("source_name", chunk.sourceName()));
    }).toList();

    System.out.println("!!! try to store");
    vectoreStore.add(docsToEmbedAndStore);

    return docsToEmbedAndStore.size();
  }
}
