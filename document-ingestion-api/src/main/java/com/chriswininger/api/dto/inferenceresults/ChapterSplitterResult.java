package com.chriswininger.api.dto.inferenceresults;

public record ChapterSplitterResult(
        ChapterSplitterAIAnalysisResult aiAnalysisResult,
        TableOfContentsAnalysis tableOfContentsAnalysis
) {}
