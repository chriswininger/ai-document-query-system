package com.chriswininger.api.dto;

import com.chriswininger.api.dto.inferenceresults.SegmentSummaryResult;

public record Segment(
        String fullSegment,
        SegmentSummaryResult segmentSummary
) {}
