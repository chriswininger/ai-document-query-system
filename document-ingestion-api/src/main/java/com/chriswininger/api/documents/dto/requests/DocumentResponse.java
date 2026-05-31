package com.chriswininger.api.documents.dto.requests;

import java.time.OffsetDateTime;
import java.util.List;

public record DocumentResponse(
        Long id,
        String title,
        String type,
        String summary,
        List<String> characters,
        String fullText,
        Integer yearPublished,
        String authorName,
        List<String> possibleQuestionsThisAnswers,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
