package com.wininger.spring_ai_demo.api;

public record ChatRequest(String userPrompt, Integer conversationId) {
}
