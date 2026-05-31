package com.chriswininger.api.documents.services;

import com.chriswininger.api.documents.dto.BookMetadataAnalysis;
import com.chriswininger.api.dto.inferenceresults.BookMetadataAnalysisResult;
import com.chriswininger.api.services.inferenceapi.OllamaApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Looks at the front and back of a book to see if there is
 * any usual information that can be learned from things like
 * the introduction, forward, or table of contents
 *
 */
@ApplicationScoped
public class BookMetaExtractionService {
    private static final Logger LOG = Logger.getLogger(BookMetaExtractionService.class);

    private final ChapterService chapterService;

    private final OllamaApiService ollamaApiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT_FRONT = """
        You are a document analysis assistant. Your job is to analyze a segment of text extracted
        from books. You will be looking at the front of the book to see what can be learned, from
        things like the copyright page, introduction, and forward. Your task is to produce a plain-text analysis
        with clearly labeled sections:
        
        Has Summary Information:
        Did we correctly provide the front half of the book, containing information such as title,
        author name, forwards and introductions?
        
        Title:
        The books full title.

        Author:
        The author's full name.
        
        Copyright:
        When was the book written: MM-YYYY
        
        Publisher:
        What company published the book.
        
        Summary:
        Summary of any interesting information about the book -- historical context, facts about the author, is this
        book part of a series and other such information.

        Respond in plain text only. Do NOT use JSON, markdown, or code fences.
        
        Characters:
        A list of any character you will find in the book, based on the introduction.
        
        POSSIBLE QUESTIONS THIS ANSWERS:
        A list of questions a curious reader might have that are directly answered by the introduction. Phrase them
        as natural questions. "What is this book about?", Who wrote this novel?". Include questions specific to this
        book, "What is {title} about?", "What books did {author} write?" List one per line.
        """;

    private static final String SYSTEM_PROMPT_BACK = """
        You are a document analysis assistant. Your job is to analyze a segment of text extracted
        from books. You will be looking at the back of the book to see what can be learned, from
        things like the copyright page, index, postscript, etc. You already know a little about the book
        from looking at the front. Here is a summary of what we found at the front of the book:
        
        ==== Summary Of Book Intro ====
        %s
        ===============================
        
        Do not use "Summary Of Book Intro" directly when analyzing the text. It's there for additional context only.
        
        Your task is to produce a plain-text analysis
        with clearly labeled sections:
        
        Has Summary Information:
        Did we correctly provide the back half of the book, containing information such as publisher, index, postscript?
        
        Copyright:
        When was the book written: MM-YYYY
        
        Publisher:
        What company published the book.
        
        Summary:
        Summary of any interesting information about the book -- historical context, facts about the author, is this
        book part of a series and other such information.

        Respond in plain text only. Do NOT use JSON, markdown, or code fences.
        
        Characters:
        A list of any character you will find in the book, based on the back of the book.
        
        POSSIBLE QUESTIONS THIS ANSWERS:
        A list of questions a curious reader might have that are directly answered by the introduction. Phrase them
        as natural questions. "What published this book?", Who wrote this novel?". Include questions specific to this
        book, "What is {title} about?", "What books did {author} write?" List one per line.
        """.trim();

    private static final String SYSTEM_MESSAGE_STRUCTURE = """
            You are a data formatting assistant. You will be given two plain-text literary analyses
            based on the front and back of a book. Each analysis contains labeled the labeled sections:
             "Has Summary Information",
             "Copyright",
             "Publisher",
             "Summary",
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

    public BookMetaExtractionService(
            final ChapterService chapterService,
            final OllamaApiService ollamaApiService
    ) {
        this.chapterService = chapterService;
        this.ollamaApiService = ollamaApiService;
    }

    public BookMetadataAnalysis extractMetaDataFromTheBook(
            final String fullBook,
            final Pattern chapterSplitter
    ) throws IOException, InterruptedException {
        final String frontText = extractFrontText(fullBook, chapterSplitter);
        final String frontAnalysis = analyzeFrontText(frontText);

        final String backText = takeSentencesFromBack(fullBook, 40);
        final String backAnalysis = analyzeBackText(backText, frontAnalysis);

        final String userMessage = """
                ===== Analysis of Front ======
                %s
                ==============================

                ===== Analysis of Back ======
                %s
                ==============================

                Based on the above analyses of both front and back please respond with structured JSON.
        """.formatted(frontAnalysis, backAnalysis).trim();

        final BookMetadataAnalysisResult result = ollamaApiService.callOllamaStructuredResponse(
                SYSTEM_MESSAGE_STRUCTURE.formatted(ollamaApiService.buildExampleJson(BookMetadataAnalysisResult.class)),
                userMessage, true, BookMetadataAnalysisResult.class);

        return new BookMetadataAnalysis(frontText, backText, result);
    }

    private String extractFrontText(
            final String fullBook,
            final Pattern chapterSplitter
    ) {
        if (Objects.nonNull(chapterSplitter)) {
            final var chapters = chapterService.splitIntoChapters(fullBook, chapterSplitter);
            LOG.infof("(extractFrontText) chapterSplitter found '%s' chapter(s)", chapters.size());
            return !chapters.isEmpty()
                    ? chapters.getFirst().content()
                    : takeSentencesFromFront(fullBook, 40);
        }
        return takeSentencesFromFront(fullBook, 40);
    }

    private String analyzeFrontText(
            final String frontText
    ) throws IOException, InterruptedException {
        final String userMessage = """
                First half of the book containing the intro and possibly a bit of the first chapter. Focus only on
                the contents before the start of the first chapter.
                
                =======================
                %s
                =======================
                """.formatted(frontText).trim();
        return ollamaApiService.callOllamaPlainTextResponse(SYSTEM_PROMPT_FRONT, userMessage, true);
    }

    private String analyzeBackText(
            final String backText, final String frontAnalysis
    ) throws IOException, InterruptedException {
        final String systemMessage = SYSTEM_PROMPT_BACK.formatted(frontAnalysis);
        final String userMessage = """
                Back half of the book containing postscript, publisher information, etc. This may also contain a bit of
                the final chapter. Focus only on the contents before the start of the first chapter.

                =======================
                %s
                =======================
                """.formatted(backText);

        return ollamaApiService.callOllamaPlainTextResponse(systemMessage, userMessage, true);
    }

    private String takeSentencesFromFront(final String fullBook, int count) {
        LOG.infof("(takeSentencesFromFront) count: %s", count);
        final Document document = Document.from(fullBook);

        DocumentBySentenceSplitter sentenceSplitter = new DocumentBySentenceSplitter(Integer.MAX_VALUE, 0);
        List<String> sentences = Arrays.stream(sentenceSplitter.split(document.text()))
                .filter(s -> !s.isBlank())
                .toList();

        return String.join(" ", sentences.subList(0, Math.min(count, sentences.size())));
    }

    private String takeSentencesFromBack(final String fullBook, int count) {
        LOG.infof("(takeSentencesFromBack) count: %s", count);
        final Document document = Document.from(fullBook);

        DocumentBySentenceSplitter sentenceSplitter = new DocumentBySentenceSplitter(Integer.MAX_VALUE, 0);
        List<String> sentences = Arrays.stream(sentenceSplitter.split(document.text()))
                .filter(s -> !s.isBlank())
                .toList();

        int size = sentences.size();
        return String.join(" ", sentences.subList(Math.max(0, size - count), size));
    }
}
