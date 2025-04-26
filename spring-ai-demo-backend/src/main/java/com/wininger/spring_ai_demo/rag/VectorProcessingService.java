package com.wininger.spring_ai_demo.rag;

import com.wininger.spring_ai_demo.rag.db.DocumentImportService;
import com.wininger.spring_ai_demo.rag.db.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VectorProcessingService {
  private static final Logger logger = LoggerFactory.getLogger(VectorProcessingService.class);
  private final DocumentImportService documentImportService;
  private final VectorStore vectorStore;

  // it might be that this table should just be managed and accessed through the framework
  // vector store, but for now we also have this for crud operations
  private final VectorStoreService vectorStoreService;

  public VectorProcessingService(
      final DocumentImportService documentImportService,
      final VectorStore vectorStore,
      final VectorStoreService vectorStoreService
  ) {
    this.documentImportService = documentImportService;
    this.vectorStore = vectorStore;
    this.vectorStoreService = vectorStoreService;
  }

  // super helpful https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html
  public int storeChunks(final int documentId) {
    logger.info("begin processing document id: '{}'", documentId);

    // drop existing rows in case this is re-processing
    vectorStoreService.deleteAllVectorsByDocumentId(documentId);

    final var chunks = documentImportService.getAllDocumentChunksByDocId(documentId);

    final List<Document> docsToEmbedAndStore = chunks.stream().map(chunk -> {
      if (chunk.content().split("\\s+").length > 1000) {
        logger.warn("Exceeding max safe doc size, performing further splits");
        // figure out a way to split up into smaller chunks here
        var splitter = new TokenTextSplitter();

        final List<Document> smallerChunks =
            splitter.apply(List.of(new Document(chunk.content(), Map.of("source_name", chunk.sourceName()))));

        logger.debug("chunks reallocation into: {} chunks", smallerChunks.size());
        return smallerChunks;
      } else {
        // normal case leave it alone
        return List.of(
            new Document(chunk.content(), Map.of(
                "source_name", chunk.sourceName(),
                "source_id", documentId,
                "chunk_id", chunk.id()))
        );
      }
    }).flatMap(List::stream)
      .toList();

    logger.debug("storing {} chunks", docsToEmbedAndStore.size());
    vectorStore.add(docsToEmbedAndStore);

    logger.info("done processing document id: '{}'", documentId);
    return docsToEmbedAndStore.size();
  }
}
