package com.chriswininger.api.dto.inferenceresults;

import com.chriswininger.ollama.InferenceDescription;

public record TableOfContentsAnalysis(
        @InferenceDescription("A boolean value indicating if the document has a table of contents.")
        boolean containsTableOfContents,

        @InferenceDescription("The complete text of the document's table of contents.")
        String tableOfConents
) { }
