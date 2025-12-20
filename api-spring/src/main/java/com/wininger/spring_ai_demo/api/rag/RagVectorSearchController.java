package com.wininger.spring_ai_demo.api.rag;

import com.wininger.spring_ai_demo.rag.QueryRewritingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rag/vectors")
@CrossOrigin("*")
public class RagVectorSearchController {
  private static final Logger log = LoggerFactory.getLogger(RagVectorSearchController.class);

  private final VectorSearchService vectorSearchService;
  private final QueryRewritingService queryRewritingService;

  public RagVectorSearchController(
      final VectorSearchService vectorSearchService,
      final QueryRewritingService queryRewritingService
  ) {
    this.vectorSearchService = vectorSearchService;
    this.queryRewritingService = queryRewritingService;
  }

  @PostMapping("/search")
  public VectorSearchResponse search(
      @RequestBody final VectorSearchRequest vectorSearchRequest,
      @RequestParam(required = false, defaultValue = "false") final boolean useRAGRewrite
  ) {
    final String originalQuery = vectorSearchRequest.query();
    final String rewrittenQuery;

    if (useRAGRewrite) {
      log.info("RAG rewrite enabled, rewriting query: '{}'", originalQuery);
      rewrittenQuery = queryRewritingService.rewriteQuerySingle(originalQuery);
      log.info("Rewritten query: '{}' -> '{}'", originalQuery, rewrittenQuery);
    } else {
      rewrittenQuery = null;
    }

    List<VectorSearchResult> results = vectorSearchService.performSearch(
        useRAGRewrite ? rewrittenQuery : originalQuery,
        vectorSearchRequest.numMatches(),
        vectorSearchRequest.documentSourceIds()
    );

    return new VectorSearchResponse(rewrittenQuery, results);
  }
}
