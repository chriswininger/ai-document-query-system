package com.wininger.spring_ai_demo.rag.dto;

import org.jooq.JSONB;

import java.time.LocalDateTime;

public record DtoDocumentImportChunk(
    int id,
    int documentImportId,
    String sourceName,
    String content,
    JSONB metadata,  // Using Jackson's JsonNode for JSONB
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
