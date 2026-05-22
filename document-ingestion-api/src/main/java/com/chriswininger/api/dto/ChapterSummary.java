package com.chriswininger.api.dto;

import com.chriswininger.api.dto.inferenceresults.ChapterSummaryResult;

import java.util.List;

public record ChapterSummary(
        int sequence,
        ChapterSummaryResult chapterSummaryResult,
        String chapterTitle,
        String fullChapterText,
        List<Segment> segments
) {}
