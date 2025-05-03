package com.wininger.spring_ai_demo.api.rag;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rag/vectors")
@CrossOrigin("*")
public class RagVectorSearchController {
  private final VectorSearchService vectorSearchService;

  public RagVectorSearchController(final VectorSearchService vectorSearchService) {
    this.vectorSearchService = vectorSearchService;
  }

  @PostMapping("/search")
  public List<VectorSearchResult> search(@RequestBody final VectorSearchRequest vectorSearchRequest) {
    return vectorSearchService.performSearch(
        vectorSearchRequest.query(),
        vectorSearchRequest.numMatches(),
        vectorSearchRequest.documentSourceIds()
    );
  }
}
