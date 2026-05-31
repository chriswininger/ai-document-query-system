package com.chriswininger.api.documents.dto;

import com.chriswininger.api.dto.inferenceresults.SegmentSummaryResult;

public record Segment(
        int sequence,
        String fullSegment,
        SegmentSummaryResult segmentSummary
) {}
