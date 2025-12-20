package com.wininger.spring_ai_demo.api.rag;

import java.util.List;

public record VectorSearchResponse(String rewrittenQuery, List<VectorSearchResult> searchResults) {}
