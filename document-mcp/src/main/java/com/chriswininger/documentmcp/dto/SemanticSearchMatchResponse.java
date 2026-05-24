package com.chriswininger.documentmcp.dto;

public record SemanticSearchMatchResponse(
        double score,
        String text,
        Long sectionId,
        Long chapterId,
        Long documentId,
        String bookTitle,
        String chapterLabel
) {}
