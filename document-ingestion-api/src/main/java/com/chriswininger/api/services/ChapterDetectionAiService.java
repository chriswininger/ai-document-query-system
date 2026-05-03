package com.chriswininger.api.services;

import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public interface ChapterDetectionAiService {

    @Description("Result of analyzing the front matter of a book to detect chapter heading patterns")
    record ChapterPatternAnalysis(
            @Description("True if the book appears to be divided into chapters or numbered sections")
            boolean isChapterBook,
            @Description("The exact string that appears at the start of each chapter heading as it appears verbatim in the text, including any leading whitespace or newline characters. For example: '\nChapter ' or '\nCHAPTER ' or '\n1.' Leave empty string if isChapterBook is false.")
            String splitString,
            @Description("2 to 3 verbatim examples of complete chapter headings exactly as they appear in the text, for example: ['Chapter 1', 'Chapter 2', 'Chapter 3']. Leave empty list if isChapterBook is false.")
            List<String> examples
    ) {}

    @SystemMessage("""
            You are a document analysis assistant. You will be given text extracted from the front
            of a book (likely via PDF-to-text conversion). Your task is to determine whether this
            is a chapter-based book and, if so, identify the exact string pattern used to begin
            each chapter heading.

            Rules:
            - isChapterBook: true if the text shows evidence of chapters, parts, or numbered sections
            - splitString: the minimal exact string that uniquely identifies the start of a chapter
              heading. It must be specific enough to avoid false matches (e.g. prefer '\nChapter '
              over just 'Chapter'). Include leading newline if chapters always start on a new line.
            - examples: copy 2-3 chapter headings verbatim from the text as they appear

            Respond only with valid JSON matching the required schema.
            """)
    @UserMessage("Analyze the following text from the front of a book:\n\n{{it}}")
    ChapterPatternAnalysis detectPattern(String frontSlice);

    @SystemMessage("""
            You are a document analysis assistant. You will be given text extracted from the front
            of a book (likely via PDF-to-text conversion). Your task is to determine whether this
            is a chapter-based book and, if so, identify the exact string pattern used to begin
            each chapter heading.

            IMPORTANT: A previous attempt to detect a chapter pattern returned the split string
            shown below, but it was NOT found in the document. You must suggest a different pattern.
            Look more carefully at the text for alternative chapter heading formats.

            Rules:
            - isChapterBook: true if the text shows evidence of chapters, parts, or numbered sections
            - splitString: the minimal exact string that uniquely identifies the start of a chapter
              heading. It must be specific enough to avoid false matches. Try a different format
              than the previous attempt.
            - examples: copy 2-3 chapter headings verbatim from the text as they appear

            Respond only with valid JSON matching the required schema.
            """)
    @UserMessage("Previous failed split string: {{previousAttempt}}\n\nAnalyze the following text from the front of a book:\n\n{{frontSlice}}")
    ChapterPatternAnalysis retryDetectPattern(@V("frontSlice") String frontSlice, @V("previousAttempt") String previousAttempt);
}
