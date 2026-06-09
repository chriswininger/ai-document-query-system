package com.chriswininger.cli.commands.query.dto;

import com.chriswininger.ollama.InferenceDescription;

public record PossibleDocument(
        @InferenceDescription("The numeric document ID (a long integer, e.g. 1, 2, 3). Must be ONLY the number, not the title or any other text.")
        long documentId,

        @InferenceDescription("Brief explanation of why this document might answer the user's question.")
        String reason
) {}
