package com.chriswininger.api.dto.inferenceresults;

public record TableOfContentsIsolationResult(
        boolean containsTableOfContents,
        String tableOfContents,
        String documentWithoutTableOfContents
) {}
