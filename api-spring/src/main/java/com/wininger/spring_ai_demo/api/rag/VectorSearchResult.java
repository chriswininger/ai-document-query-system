package com.wininger.spring_ai_demo.api.rag;

import java.util.Map;

public record VectorSearchResult(String text, Map<String, Object> metadata, Double score) {}
