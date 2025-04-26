package com.wininger.spring_ai_demo.rag.dto;

import org.jooq.JSON;

import java.util.UUID;

public record DtoVectorStore (
    UUID id,
    String content,
    JSON metadata,
    float[] embedding
) {}
