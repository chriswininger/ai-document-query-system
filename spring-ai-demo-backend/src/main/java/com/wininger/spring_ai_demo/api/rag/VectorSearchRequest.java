package com.wininger.spring_ai_demo.api.rag;

import java.util.List;

public record VectorSearchRequest(String query, Integer numMatches, List<Integer> documentSourceIds)
{}
