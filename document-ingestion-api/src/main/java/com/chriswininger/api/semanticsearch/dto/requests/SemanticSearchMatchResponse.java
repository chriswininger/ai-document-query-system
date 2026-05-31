package com.chriswininger.api.semanticsearch.dto.requests;

public record SemanticSearchMatchResponse(
        double score,
        String text,
        Long sectionId,
        Long chapterId,
        Long documentId,
        String bookTitle,
        String chapterLabel
) {}
