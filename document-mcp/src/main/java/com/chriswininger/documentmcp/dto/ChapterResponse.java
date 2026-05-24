package com.chriswininger.documentmcp.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ChapterResponse(
        Long id,
        Long documentId,
        String chapterTitle,
        Integer sequence,
        String summary,
        List<String> characters,
        String fullText,
        List<String> possibleQuestionsThisAnswers,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
