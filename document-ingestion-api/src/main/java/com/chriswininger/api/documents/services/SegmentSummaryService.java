package com.chriswininger.api.documents.services;

import com.chriswininger.api.documents.dto.Segment;
import com.chriswininger.api.dto.inferenceresults.BookSummaryResult;
import com.chriswininger.api.dto.inferenceresults.ChapterSummaryResult;
import com.chriswininger.api.dto.inferenceresults.SegmentSummaryResult;
import com.chriswininger.ollama.OllamaApiService;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SegmentSummaryService {
    private static final Logger LOG = Logger.getLogger(SegmentSummaryService.class);

    private final OllamaApiService ollamaApiService;

    private final DocumentChunkerService documentChunkerService;

    private static final String SYSTEM_PROMPT_UNSTRUCTURED = """
        You are a literary analysis assistant. You will be given a segment of text from a chapter
        of a book, along with context about the book and the chapter it belongs to. Your task is
        to produce a plain-text analysis of the segment with clearly labeled sections:

        SUMMARY:
        A concise paragraph summarizing the key events, ideas, or arguments in this segment.
        Focus on what happens and why it matters to the narrative. Use the provided book and
        chapter context to inform your understanding, but only summarize what appears in the
        segment itself.

        CHARACTERS:
        A list of every character (person, creature, or named entity) who appears or is
        meaningfully mentioned in this segment. List each character only once. For example:
        Mr Dillon, James Dillon, James and Dillon should be listed only as James Dillon if
        they all refer to the same character. List one character per line.

        POSSIBLE QUESTIONS THIS ANSWERS:
        A list of questions a curious reader might have that this segment directly answers.
        Phrase them as natural questions. List one question per line.

        Respond in plain text only. Do NOT use JSON, markdown, or code fences.
        """;

    private static final String SYSTEM_PROMPT_STRUCTURED = """
        You are a data formatting assistant. You will be given a plain-text literary analysis
        of a segment from a book chapter. The analysis contains labeled sections:
         "SUMMARY",
         "CHARACTERS",
         "POSSIBLE QUESTIONS THIS ANSWERS"

        Your job is to convert this text into a JSON object.

        Provide the following fields:
        ```
        %s
        ```

        Rules:
        - Do not add, remove, or rephrase any content. Faithfully convert what is given.
        - Respond with ONLY the JSON object. No markdown, no explanation, no code fences.
        """.trim();

    public SegmentSummaryService(
            final OllamaApiService ollamaApiService,
            final DocumentChunkerService documentChunkerService
    ) {
        this.ollamaApiService = ollamaApiService;
        this.documentChunkerService = documentChunkerService;
    }

    public List<Segment> summarizeSegments(
            final String text,
            final ChapterSummaryResult chapterSummary,
            final BookSummaryResult bookSummary
    ) throws IOException, InterruptedException {
        final List<String> chunks = documentChunkerService.chunkText(text);
        LOG.infof("(summarizeSegments) chunked text into %d segments", chunks.size());

        final List<Segment> results = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            LOG.infof("(summarizeSegments) summarizing segment %d of %d", i + 1, chunks.size());
            final var summary = summarizeSegment(chunks.get(i), chapterSummary, bookSummary);
            LOG.infof("(summarizeSegments) summary: %s", summary);
            results.add(new Segment(i, chunks.get(i), summary));
        }

        return results;
    }

    public SegmentSummaryResult summarizeSegment(
            final String segmentText,
            final ChapterSummaryResult chapterSummary,
            final BookSummaryResult bookSummary
    ) throws IOException, InterruptedException {
        final String plainTextAnalysis = summarizeUnstructured(segmentText, chapterSummary, bookSummary);

        LOG.infof("(summarizeSegment) unstructured pass complete, running structured pass");

        return summarizeStructured(plainTextAnalysis);
    }

    private String summarizeUnstructured(
            final String segmentText,
            final ChapterSummaryResult chapterSummary,
            final BookSummaryResult bookSummary
    ) throws IOException, InterruptedException {
        final String chapterCharacters = chapterSummary.characters() != null
                ? String.join(", ", chapterSummary.characters())
                : "none identified";

        final String chapterQuestions = chapterSummary.possibleQuestionsThisAnswers() != null
                ? String.join("\n", chapterSummary.possibleQuestionsThisAnswers())
                : "none identified";

        final String bookCharacters = bookSummary.characters() != null
                ? String.join(", ", bookSummary.characters())
                : "none identified";

        final String userMessage = """
                ===== Book Context =====
                Title: %s
                Author: %s
                Book Summary: %s
                Characters in book: %s
                ========================

                ===== Chapter Context =====
                Chapter Summary: %s
                Characters in chapter: %s
                Questions this chapter answers:
                %s
                ============================

                ===== Segment Text =====
                %s
                ========================

                Based on the segment text above, and using the book and chapter context to \
                inform your understanding, produce an analysis of this segment.
                """.formatted(
                bookSummary.title(),
                bookSummary.authorName(),
                bookSummary.summary(),
                bookCharacters,
                chapterSummary.summary(),
                chapterCharacters,
                chapterQuestions,
                segmentText
        ).trim();

        return ollamaApiService.callOllamaPlainTextResponse(SYSTEM_PROMPT_UNSTRUCTURED, userMessage, true);
    }

    private SegmentSummaryResult summarizeStructured(
            final String plainTextAnalysis
    ) throws IOException, InterruptedException {
        final String userMessage = """
                ===== Segment Analysis =====
                %s
                ============================

                Based on the above analysis please respond with structured JSON.
                """.formatted(plainTextAnalysis).trim();

        return ollamaApiService.callOllamaStructuredResponse(
                SYSTEM_PROMPT_STRUCTURED.formatted(ollamaApiService.buildExampleJson(SegmentSummaryResult.class)),
                userMessage, true, SegmentSummaryResult.class);
    }
}
