package com.wininger.spring_ai_demo.api.chat;

import com.wininger.spring_ai_demo.api.rag.VectorSearchResult;

import java.util.Date;
import java.util.List;

public record ChatResponse(
    String prompt,
    String response,
    List<VectorSearchResult> vectorSearchResults,
    String model,
    int conversationId,
    Date requestTimeStartTime,
    Date requestEndTime
) {}
