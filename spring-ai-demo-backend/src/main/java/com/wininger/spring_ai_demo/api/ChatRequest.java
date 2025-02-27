package com.wininger.spring_ai_demo.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatRequest(
    String userPrompt,
    Integer conversationId,
    @JsonProperty(required = false)
    String systemPrompt) {}
