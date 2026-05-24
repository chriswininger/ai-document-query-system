package com.chriswininger.documentmcp.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record BookMetadataResponse(
        Long id,
        Long documentId,
        String summary,
        String title,
        String authorName,
        String publisher,
        Integer yearPublished,
        List<String> characters,
        List<String> possibleQuestionsThisAnswers,
        Boolean hasSummaryInformation,
        String fullTextFront,
        String fullTextBack,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
