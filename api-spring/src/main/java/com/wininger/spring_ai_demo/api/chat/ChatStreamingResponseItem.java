package com.wininger.spring_ai_demo.api.chat;

import java.util.Date;

public record ChatStreamingResponseItem(
    String model,
    int conversationId,
    ChatStreamingResponseItemType itemType,
    String output
) {}
