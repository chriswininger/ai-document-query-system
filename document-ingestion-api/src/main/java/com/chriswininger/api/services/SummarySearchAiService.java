package com.chriswininger.api.services;

import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface SummarySearchAiService {

    @Description("Result of analyzing a text segment for summary information")
    record SummaryAnalysis(
            @Description("True if the segment contains meta information about what the book is about, such as a preface, introduction, conclusion, or back-cover-style description")
            boolean hasSummaryInformation,
            @Description("True if the segment feels like it is in the middle of something — for example, a passage that starts or ends without context, a list that appears incomplete, a paragraph that begins mid-thought, or content that strongly implies more relevant text exists just before or after this segment. Set to true whenever there is a reasonable chance that expanding the slice would reveal additional summary information.")
            boolean isCutOff,
            @Description("A verbatim copy of all text in the segment that qualifies as summary or meta information about the book. Include titles, series names, author notes, prefaces, publisher info, dedications, blurbs, and similar content exactly as they appear. Leave empty string if hasSummaryInformation is false.")
            String summaryInformation
    ) {}

    @SystemMessage("""
            You are a document analysis assistant. Your job is to analyze a segment of text extracted
            from a book (likely via PDF-to-text conversion) and determine three things:

            1. hasSummaryInformation: Set to true if the segment contains ANY of the following:
               - The book title, series name, or author name presented as a heading
               - A table of contents or chapter listing
               - A preface, foreword, introduction, or author's note explaining the book
               - A conclusion, afterword, or epilogue
               - Publisher information, copyright notice, or ISBN (indicates front/back matter)
               - Praise, blurbs, or reviews describing the book
               - A dedication or epigraph
               Be generous — if there is any doubt, set this to true.

            2. isCutOff: Set to true if the segment feels like it is in the middle of something —
               for example a list that seems incomplete, a passage that starts or ends without context,
               content that implies more relevant text exists just before or after it, or any case where
               expanding the slice would likely reveal more summary information. Do NOT require a
               literal mid-sentence cut — think about whether the boundaries of this excerpt feel
               natural or abrupt.

            3. summaryInformation: Copy verbatim all text from the segment that qualifies as summary
               or meta information (titles, series names, author notes, prefaces, publisher details,
               dedications, blurbs, etc.). Do not paraphrase or modify — reproduce the text exactly
               as it appears. Set to empty string if hasSummaryInformation is false.

            Respond only with valid JSON matching the required schema.
            """)
    @UserMessage("Analyze the following text segment:\n\n{{it}}")
    SummaryAnalysis analyze(String textSegment);

    @SystemMessage("""
            You are a document analysis assistant. You will be given raw text extracted from the
            front and/or back matter of a book. Your task is to produce a clean, concise summary
            of what the book is about, based solely on the provided meta content.
            Use only the information present — do not invent or infer beyond what is given.
            """)
    @UserMessage("Produce a summary of this book based on the following meta content:\n\n{{it}}")
    String summaryFromMetaContents(String metaContents);
}
