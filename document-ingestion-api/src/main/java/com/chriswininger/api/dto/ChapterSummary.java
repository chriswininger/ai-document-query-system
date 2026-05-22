package com.chriswininger.api.dto;

import com.chriswininger.api.dto.inferenceresults.ChapterSummaryResult;

import java.util.List;

public record ChapterSummary(
        ChapterSummaryResult chapterSummaryResult,
        String fullChapterText,
        List<Segment> segments
) {}
