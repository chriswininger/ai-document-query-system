package com.chriswininger.api.dto.inferenceresults;

import java.util.List;

/**
 * Stores information gleamed from scanning the front and back of a book
 */
public record BookMetadataAnalysisResult(
        @InferenceDescription("A summary of everything learned from reading the front and back of the book.")
        String summary,

        @InferenceDescription("The books full title.")
        String title,

        @InferenceDescription("The Author's name")
        String authorName,

        @InferenceDescription("The publisher.")
        String publisher,

        @InferenceDescription("The year the book was published.")
        Long yearPublished,

        @InferenceDescription("A list of any character you will find in the book, based on the back of the book.")
        List<String> characters,

        @InferenceDescription("A list of questions a curious reader might have that are directly answered by the material analyzed.")
        List<String> possibleQuestionsThisAnswers,

        @InferenceDescription("Did contents supplied for analysis actually contain summary information")
        Boolean hasSummaryInformation
) {}
