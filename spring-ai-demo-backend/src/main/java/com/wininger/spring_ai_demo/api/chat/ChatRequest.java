package com.wininger.spring_ai_demo.api.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatRequest(
    String userPrompt,
    Integer conversationId,
    @JsonProperty(required = false)
    String systemPrompt,
    @JsonProperty(required = false)
    List<Integer> documentSourceIds) {}
