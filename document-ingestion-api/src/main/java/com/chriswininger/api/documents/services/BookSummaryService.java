package com.chriswininger.api.documents.services;

import com.chriswininger.api.dto.inferenceresults.BookSummaryResult;
import com.chriswininger.ollama.OllamaApiService;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ApplicationScoped
public class BookSummaryService {
    private static final Logger LOG = Logger.getLogger(BookSummaryService.class);

    private final OllamaApiService ollamaApiService;

    private static final String SYSTEM_PROMPT_UNSTRUCTURED = """
        You are a literary analysis assistant. You will be given a summary of the front and back
        of a book along with summaries of each chapter. Your task is to produce a comprehensive
        plain-text analysis of the entire book with clearly labeled sections:

        Summary:
        A thorough summary of the book as a whole, synthesizing what is learned from the front/back
        matter and all chapter summaries. Cover the major themes, plot arc, key arguments, and
        overall narrative. This should read as a cohesive overview, not a chapter-by-chapter recap.

        Title:
        The book's full title.

        Author:
        The author's full name.

        Publisher:
        What company published the book.

        Year Published:
        The year the book was published.

        Characters:
        A consolidated list of every character (person, creature, or named entity) who appears
        across the entire book. List each character only once. List one character per line.

        POSSIBLE QUESTIONS THIS ANSWERS:
        A list of questions a curious reader might have that are directly answered by the book
        as a whole. Phrase them as natural questions. Include both general questions ("What is
        this book about?") and specific ones. List one question per line.

        Respond in plain text only. Do NOT use JSON, markdown, or code fences.
        """;

    private static final String SYSTEM_PROMPT_STRUCTURED = """
        You are a data formatting assistant. You will be given a plain-text literary analysis
        of an entire book. The analysis contains labeled sections:
         "Summary",
         "Title",
         "Author",
         "Publisher",
         "Year Published",
         "Characters",
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

    public BookSummaryService(final OllamaApiService ollamaApiService) {
        this.ollamaApiService = ollamaApiService;
    }

    public BookSummaryResult summarizeBook(
            final String frontBackSummary,
            final List<String> chapterSummaries
    ) throws IOException, InterruptedException {
        final String plainTextSummary = summarizeUnstructured(frontBackSummary, chapterSummaries);

        LOG.infof("(summarizeBook) unstructured pass complete, running structured pass");

        return summarizeStructured(plainTextSummary);
    }

    private String summarizeUnstructured(
            final String frontBackSummary,
            final List<String> chapterSummaries
    ) throws IOException, InterruptedException {
        final String chaptersBlock = IntStream.range(0, chapterSummaries.size())
                .mapToObj(i -> "--- Chapter %d ---\n%s".formatted(i + 1, chapterSummaries.get(i)))
                .collect(Collectors.joining("\n\n"));

        final String userMessage = """
                ===== Front/Back Summary =====
                %s
                ==============================

                ===== Chapter Summaries =====
                %s
                ==============================

                Based on all of the above, produce a comprehensive analysis of the entire book.
                """.formatted(frontBackSummary, chaptersBlock).trim();

        return ollamaApiService.callOllamaPlainTextResponse(SYSTEM_PROMPT_UNSTRUCTURED, userMessage, true);
    }

    private BookSummaryResult summarizeStructured(
            final String plainTextSummary
    ) throws IOException, InterruptedException {
        final String userMessage = """
                ===== Book Analysis =====
                %s
                =========================

                Based on the above analysis please respond with structured JSON.
                """.formatted(plainTextSummary).trim();

        return ollamaApiService.callOllamaStructuredResponse(
                SYSTEM_PROMPT_STRUCTURED.formatted(ollamaApiService.buildExampleJson(BookSummaryResult.class)),
                userMessage, true, BookSummaryResult.class);
    }
}
