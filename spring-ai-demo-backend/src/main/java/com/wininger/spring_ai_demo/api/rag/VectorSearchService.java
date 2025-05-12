package com.wininger.spring_ai_demo.api.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {
  private final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);

  private final VectorStore vectorStore;

  public VectorSearchService(final VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  public List<VectorSearchResult> performSearch(
      final String query,
      final int topK,
      List<Integer> documentSourceIds
  ) {
    final String filterExpression = documentSourceIds.stream()
        .map("source_id == %s"::formatted)
        .collect(Collectors.joining(" || "));

    logger.debug("filter expression: '{}'", filterExpression);
    logger.debug("query: '{}'", query);
    logger.debug("topK: '{}'", topK);

    final var searchRequest = SearchRequest.builder()
        .topK(topK)
        .filterExpression(filterExpression)
        .query(query)
        .build();

    return vectorStore.similaritySearch(searchRequest)
        .stream()
        .map(r -> new VectorSearchResult(r.getText(), r.getMetadata(), r.getScore()))
        .toList();
  }
}
