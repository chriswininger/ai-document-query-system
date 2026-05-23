package com.chriswininger.api.dto.requests;

import java.time.OffsetDateTime;
import java.util.List;

public record SectionResponse(
        Long id,
        Long chapterId,
        Integer sequence,
        String summary,
        List<String> characters,
        String fullText,
        List<String> possibleQuestionsThisAnswers,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
