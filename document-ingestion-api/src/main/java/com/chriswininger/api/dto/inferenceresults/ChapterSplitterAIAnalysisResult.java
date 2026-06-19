package com.chriswininger.api.dto.inferenceresults;

import com.chriswininger.ollama.InferenceDescription;

public record ChapterSplitterAIAnalysisResult(
        @InferenceDescription("A Java regular expression that matches chapter headings in the book text. "
                + "Examples: \"CHAPTER .*\\n\\n\", \"[*]\\s[*]\\s[*]\\n\", \"Chapter .*\\n\".")
        String splitExpression
) {}
