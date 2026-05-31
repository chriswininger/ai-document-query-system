package com.chriswininger.api.documents.dto;

import com.chriswininger.api.dto.inferenceresults.BookMetadataAnalysisResult;

public record BookMetadataAnalysis(
        String fullFrontText,
        String fullBackText,
        BookMetadataAnalysisResult bookMetadataAnalysisResult
) {}
