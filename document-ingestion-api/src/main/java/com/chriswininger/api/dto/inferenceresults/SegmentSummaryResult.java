package com.chriswininger.api.dto.inferenceresults;

import com.chriswininger.ollama.InferenceDescription;

import java.util.List;

/**
 * Stores information produced from analyzing a segment (portion) of a chapter
 */
public record SegmentSummaryResult(
        @InferenceDescription("A concise summary of this segment's content.")
        String summary,

        @InferenceDescription("A list of characters who appear or are mentioned in this segment.")
        List<String> characters,

        @InferenceDescription("A list of questions a curious reader might have that are directly answered by this segment.")
        List<String> possibleQuestionsThisAnswers
) {}
