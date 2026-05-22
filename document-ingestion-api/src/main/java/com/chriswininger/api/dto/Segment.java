package com.chriswininger.api.dto;

import com.chriswininger.api.dto.inferenceresults.SegmentSummaryResult;

public record Segment(
        int sequence,
        String fullSegment,
        SegmentSummaryResult segmentSummary
) {}
