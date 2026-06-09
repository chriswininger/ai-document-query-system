package com.chriswininger.cli.commands.query.dto;

import com.chriswininger.ollama.InferenceDescription;

import java.util.List;

public record PossibleDocumentsResult(
        @InferenceDescription("Documents that could possibly answer the user's question. Include only documents where the evaluation was yes.")
        List<PossibleDocument> possibleDocuments
) {}
