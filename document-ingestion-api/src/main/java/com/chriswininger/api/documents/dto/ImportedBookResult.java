package com.chriswininger.api.documents.dto;

import com.chriswininger.api.dto.inferenceresults.BookSummaryResult;

import java.util.List;

public record ImportedBookResult(
        BookSummaryResult bookSummary,
        BookMetadataAnalysis bookMetadataAnalysis,
        List<ChapterSummary> chapterSummaries
) {}
