package com.wininger.spring_ai_demo.api.chat;

import com.wininger.spring_ai_demo.api.rag.VectorSearchResult;

import java.util.Date;
import java.util.List;

public record ChatStreamingResponseItem(
    String model,
    int conversationId,
    ChatStreamingResponseItemType itemType,
    String output,
    VectorSearchResult vectorSearchResult // only applies to RAG_DOCUMENT, otherwise null
) {}
