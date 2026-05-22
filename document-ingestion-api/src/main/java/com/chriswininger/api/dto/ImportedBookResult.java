package com.chriswininger.api.dto;

import com.chriswininger.api.dto.inferenceresults.BookSummaryResult;

import java.util.List;

public record ImportedBookResult(
        BookSummaryResult bookSummary,
        BookMetadataAnalysis bookMetadataAnalysis,
        List<ChapterSummary> chapterSummaries
) {}
