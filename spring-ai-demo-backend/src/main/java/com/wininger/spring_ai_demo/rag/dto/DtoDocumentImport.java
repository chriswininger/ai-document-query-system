package com.wininger.spring_ai_demo.rag.dto;

import org.jooq.JSONB;

import java.time.LocalDateTime;

public record DtoDocumentImport(
    long id,
    String sourceName,
    String nonChunkedContent,
    JSONB metadata,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
