package com.chriswininger.api.documents.services;

import com.chriswininger.api.dto.inferenceresults.ChapterSplitterAIAnalysisResult;
import com.chriswininger.api.dto.inferenceresults.ChapterSplitterResult;
import com.chriswininger.api.dto.inferenceresults.TableOfContentsAnalysis;
import com.chriswininger.ollama.OllamaApiService;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@ApplicationScoped
public class ChapterSplitAIService {
    private static final Logger LOG = Logger.getLogger(ChapterSplitAIService.class);

    private static final int AI_DETECTION_FRONT_SKIP = 10000;
    private static final int AI_DETECTION_SAMPLE_SIZE = 16000;

    private static final int MAX_RETRIES = 3;
    private static final int MIN_MATCHES = 2;
    private static final int MAX_MATCHES_PER_1000_CHARS = 2;
    private static final double MAX_EMPTY_CHAPTER_RATIO = 0.30;

    private final OllamaApiService ollamaApiService;

    private static final String SYSTEM_PROMPT_UNSTRUCTURED = """
        You are a document structure analyst. You will be given text from the beginning of a book.
        Your task is to find the pattern used for chapter headings so the book can be split into chapters.

        CRITICAL RULES:
        - The regex MUST start with \\n so it only matches headings on their own line, not
          references to chapters in the middle of a sentence.
        - Use .* (greedy) to match the variable part. NEVER use .*? (lazy).
        - The regex MUST end with \\n to capture the full heading line.
        - Do NOT use ^ or $ anchors.
        - Chapter numbers may be words (One, Two), Roman numerals (I, II, III), or digits (1, 2).
          Use .* to match any of these. Do NOT use \\d+.

        Example regex patterns that work well:
        - \\nCHAPTER .*\\n   matches lines like "CHAPTER ONE", "CHAPTER 1", "CHAPTER TWO"
        - \\nChapter .*\\n   matches lines like "Chapter 1: The Beginning", "Chapter III."
        - \\n[*]\\s[*]\\s[*]\\n   matches scene break markers "* * *" on their own line

        Respond with:

        Chapter Heading Examples:
        Copy 3 to 5 chapter headings exactly as they appear in the text, one per line.

        Pattern Description:
        Describe in one sentence what the chapter headings look like.

        Split Regex:
        Write a single Java regular expression that matches every chapter heading in this book.
        The regex MUST start with \\n and end with \\n.
        Use .* for the variable part. Do NOT use \\d+, ^, $, or .*? (lazy).
        Write ONLY the regex, nothing else.

        Respond in plain text only. Do NOT use JSON, markdown, or code fences.
        """;

    private static final String SYSTEM_PROMPT_RETRY = """
        You are a document structure analyst. You will be given text from the beginning of a book.
        Your task is to find the pattern used for chapter headings so the book can be split into chapters.

        A PREVIOUS ATTEMPT FAILED. The regex shown below did not work:
        Previous regex: %s
        Reason it failed: %s

        You must suggest a DIFFERENT regex. Learn from the failure.

        CRITICAL RULES:
        - The regex MUST start with \\n so it only matches headings on their own line.
        - Use .* (greedy) to match the variable part. NEVER use .*? (lazy).
        - The regex MUST end with \\n to capture the full heading line.
        - Do NOT use ^ or $ anchors.
        - Use .* for variable parts (numbers, titles). NEVER use \\d+.
        - The regex must work with Java's Pattern.compile().

        Example regex patterns that work well:
        - \\nCHAPTER .*\\n   matches lines like "CHAPTER ONE", "CHAPTER 1", "CHAPTER TWO"
        - \\nChapter .*\\n   matches lines like "Chapter 1: The Beginning", "Chapter III."
        - \\n[*]\\s[*]\\s[*]\\n   matches scene break markers "* * *" on their own line

        Respond with:

        Chapter Heading Examples:
        Copy 3 to 5 chapter headings exactly as they appear in the text, one per line.

        Pattern Description:
        Describe in one sentence what the chapter headings look like.

        Split Regex:
        Write a single Java regular expression that matches every chapter heading in this book.
        The regex MUST start with \\n and end with \\n.
        Write ONLY the regex, nothing else.

        Respond in plain text only. Do NOT use JSON, markdown, or code fences.
        """;

    private static final String SYSTEM_PROMPT_STRUCTURED = """
        You are a data formatting assistant. You will be given a plain-text analysis of chapter
        heading patterns found in a book. The analysis contains:
        - "Chapter Heading Examples" with verbatim examples
        - "Pattern Description" describing the headings
        - "Split Regex" containing a Java regular expression

        Your job is to extract the regex and return it as a JSON object.

        Provide the following fields:
        ```
        %s
        ```

        Rules:
        - Copy the regex from "Split Regex" exactly as given. Do not modify it.
        - The regex must be valid for Java's Pattern.compile().
        - Respond with ONLY the JSON object. No markdown, no explanation, no code fences.
        """.trim();

    private static final String SYSTEM_PROMPT_DETECT_TABLE_OF_CONTENTS = """
    You are a document structure analyst. You need to first determine if the document you
    are given has a table of contents and if so what is included in the table of contents.
    """.trim();

    private static final String SYSTEM_PROMPT_DETECT_TABLE_OF_CONTENTS_STRUCTURED = """
    You are a data formatting assistant. You will be given a plain-text analysis of a document
    that explains if the document has a table of contents. When a table of contents was identified
    it will include the text of this table. Your job is to extract a boolean value indicating whether
    a table of contents was found (true/false) along with the full text of the table when found. You
    must extract these into a JSON object with the following fields:
    ```
    %s
    ```
    
    Rules:
    - Respond with ONLY the JSON object. No markdown, no explanation, no code fences.
    """.trim();

    public ChapterSplitAIService(final OllamaApiService ollamaApiService) {
        this.ollamaApiService = ollamaApiService;
    }

    public ChapterSplitterResult detectSplitExpression(
            final String document
    ) throws IOException, InterruptedException {

        final int safeStart = 0;
        // Math.min(AI_DETECTION_FRONT_SKIP, Math.max(0, document.length() - AI_DETECTION_SAMPLE_SIZE));
        String promptSample = document.substring(safeStart,
                Math.min(document.length(), safeStart + AI_DETECTION_SAMPLE_SIZE));

        final var tableOfContentsResults = findTableOfContents(promptSample);
        if (tableOfContentsResults.containsTableOfContents()) {
            // strip the table of contents before looking for chapters
            final int initialLen = promptSample.length();
            promptSample = promptSample.replace(tableOfContentsResults.tableOfConents(), "");

            LOG.infof("Removed table of contents for chapter analysis. Before Len '%s', After Len: '%s",
                    initialLen, promptSample.length());
        }

        LOG.infof("(detectSplitPattern) sampling chars %d–%d of %d",
                safeStart, safeStart + promptSample.length(), document.length());

        final String plainTextAnalysis = analyzeUnstructured(promptSample);
        LOG.infof("(detectSplitExpression) unstructured pass complete, running structured pass");

        final ChapterSplitterAIAnalysisResult result = autoCorrectExpression(analyzeStructured(plainTextAnalysis));
        final ValidationOutcome validation = validate(result, document);

        if (validation.valid) {
            LOG.infof("(detectSplitExpression) regex validated on first attempt: %s (%d matches)",
                    result.splitExpression(), validation.matchCount);
            return new ChapterSplitterResult(result, tableOfContentsResults);
        }

        return retryWithFeedback(
                promptSample,
                document,
                tableOfContentsResults,
                result.splitExpression(),
                validation.failureReason);
    }

    private ChapterSplitterResult retryWithFeedback(
            final String promptSample,
            final String fullDocument,
            final TableOfContentsAnalysis tableOfContentsAnalysis,
            final String previousRegex,
            final String failureReason
    ) throws IOException, InterruptedException {
        String lastRegex = previousRegex;
        String lastReason = failureReason;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            LOG.infof("(retryWithFeedback) retry %d/%d — previous regex: %s — reason: %s",
                    attempt, MAX_RETRIES, lastRegex, lastReason);

            final String retryAnalysis = analyzeRetry(promptSample, lastRegex, lastReason);
            final ChapterSplitterAIAnalysisResult result = autoCorrectExpression(analyzeStructured(retryAnalysis));
            final ValidationOutcome validation = validate(result, fullDocument);

            if (validation.valid) {
                LOG.infof("(retryWithFeedback) regex validated on retry %d: %s (%d matches)",
                        attempt, result.splitExpression(), validation.matchCount);
                return new ChapterSplitterResult(result, tableOfContentsAnalysis);
            }

            lastRegex = result.splitExpression();
            lastReason = validation.failureReason;
        }

        LOG.warnf("(retryWithFeedback) all %d retries exhausted, returning last result with regex: %s",
                MAX_RETRIES, lastRegex);

        return new ChapterSplitterResult(new ChapterSplitterAIAnalysisResult(lastRegex),  tableOfContentsAnalysis);
    }

    private static ChapterSplitterAIAnalysisResult autoCorrectExpression(final ChapterSplitterAIAnalysisResult result) {
        if (result != null && result.splitExpression() != null && result.splitExpression().contains(".*?")) {
            final String corrected = result.splitExpression().replace(".*?", ".*");
            LOG.infof("(autoCorrectExpression) replaced lazy .*? with greedy .* : %s -> %s",
                    result.splitExpression(), corrected);
            return new ChapterSplitterAIAnalysisResult(corrected);
        }
        return result;
    }

    private ValidationOutcome validate(final ChapterSplitterAIAnalysisResult result, final String text) {
        if (result == null || result.splitExpression() == null || result.splitExpression().isBlank()) {
            return ValidationOutcome.failure(0, "The model returned a null or empty regex.");
        }

        final String expr = result.splitExpression();

        if (expr.startsWith("^")) {
            return ValidationOutcome.failure(0,
                    "The regex starts with ^ which only matches the very beginning of the text, not each chapter heading. "
                            + "Remove the ^ anchor and start the regex with \\n instead.");
        }

        final Pattern pattern;
        try {
            pattern = Pattern.compile(expr);
        } catch (final PatternSyntaxException e) {
            return ValidationOutcome.failure(0,
                    "The regex failed to compile: %s".formatted(e.getMessage()));
        }

        final Matcher matcher = pattern.matcher(text);
        int matchCount = 0;
        final List<String> matchSamples = new ArrayList<>();
        while (matcher.find()) {
            matchCount++;
            if (matchSamples.size() < 5) {
                matchSamples.add(matcher.group().trim());
            }
        }

        if (matchCount < MIN_MATCHES) {
            final String headingSamples = extractHeadingCandidates(text);
            return ValidationOutcome.failure(matchCount,
                    ("The regex compiled but only matched %d time(s) in the text. At least %d matches are expected. "
                            + "Here are some lines from the text that look like chapter headings: %s")
                            .formatted(matchCount, MIN_MATCHES, headingSamples));
        }

        final int maxExpected = Math.max(MIN_MATCHES, (text.length() / 1000) * MAX_MATCHES_PER_1000_CHARS);
        if (matchCount > maxExpected) {
            return ValidationOutcome.failure(matchCount,
                    ("The regex matched %d times which is far too many — it is likely matching chapter references "
                            + "inside body text, not just chapter headings. The regex must start with \\n so it only "
                            + "matches headings that appear on their own line. Some of the matches were: %s")
                            .formatted(matchCount, String.join(", ", matchSamples)));
        }

        final double emptyRatio = computeEmptyChapterRatio(pattern, text);
        if (emptyRatio > MAX_EMPTY_CHAPTER_RATIO) {
            final int emptyPercent = (int) (emptyRatio * 100);
            return ValidationOutcome.failure(matchCount,
                    ("The regex matched %d times but %d%% of the resulting chapters have little or no content. "
                            + "This usually means the regex is matching table of contents entries in addition to "
                            + "actual chapter headings. The book likely has a table of contents where lines like "
                            + "'Chapter I. Title Here' list chapters, AND separate actual chapter headings like "
                            + "'Chapter I.' (without the title on the same line). Try a more specific regex that "
                            + "only matches the actual chapter headings, not the TOC entries. For example, if "
                            + "actual headings end with a period and newline, try: \\nChapter [IVXLCDM]+\\.\\n")
                            .formatted(matchCount, emptyPercent));
        }

        return ValidationOutcome.success(matchCount);
    }

    private static double computeEmptyChapterRatio(final Pattern pattern, final String text) {
        final Matcher matcher = pattern.matcher(text);
        int totalSegments = 0;
        int emptySegments = 0;
        int lastEnd = 0;
        boolean foundFirst = false;

        while (matcher.find()) {
            if (foundFirst) {
                final String body = text.substring(lastEnd, matcher.start());
                totalSegments++;
                if (body.trim().length() < 100) {
                    emptySegments++;
                }
            }
            foundFirst = true;
            lastEnd = matcher.end();
        }

        if (foundFirst && lastEnd < text.length()) {
            final String tail = text.substring(lastEnd);
            totalSegments++;
            if (tail.trim().length() < 100) {
                emptySegments++;
            }
        }

        return totalSegments == 0 ? 0.0 : (double) emptySegments / totalSegments;
    }

    private static String extractHeadingCandidates(final String text) {
        final Pattern candidatePattern = Pattern.compile(
                "(?i)^\\s*(chapter|part|book|section|act|prologue|epilogue)\\b.*$",
                Pattern.MULTILINE);
        final Matcher matcher = candidatePattern.matcher(text);
        final List<String> candidates = new ArrayList<>();
        while (matcher.find() && candidates.size() < 5) {
            candidates.add(matcher.group().trim());
        }
        return candidates.isEmpty() ? "(none detected)" : String.join(", ", candidates);
    }

    private String analyzeUnstructured(final String frontText) throws IOException, InterruptedException {
        final String userMessage = """
                ===== Book Text (beginning) =====
                %s
                =================================

                Based on the text above, identify the chapter heading pattern and provide a Java regex \
                that matches all chapter headings.
                """.formatted(frontText).trim();

        return ollamaApiService.callOllamaPlainTextResponse(SYSTEM_PROMPT_UNSTRUCTURED, userMessage, true);
    }

    public TableOfContentsAnalysis findTableOfContents(final String frontText) throws IOException, InterruptedException {
        final String userMessage = """
                ===== Book Text (beginning) =====
                %s
                =================================
                
                Based on the text above, identify the if there is a table of contents. Return the full text of the
                table of contents if it exists.
                
                Example 1:
                
                isTableOfContents: Yes, I see a table of contents
                
                ==== Contents of Context ====
                Chapter 1 -- Of Cats and Dogs
                Chapter 2 -- Where All Things End
                Chapter 3 -- Me My Truck and Irene
                Chapter 4 -- If It Fits It 'Aint Broken
                =============================
                
                Example 2:
                
                isTableOfContents: No, there is no table of contents
                
                ==== Contents of Context ====
                No table of contents was found
                =============================
                """.formatted(frontText);

        final var plainTextAnalysis = ollamaApiService.callOllamaPlainTextResponse(SYSTEM_PROMPT_DETECT_TABLE_OF_CONTENTS, userMessage, true);


        final String structuringMessage = """
                # Document Analysis
                
                ```
                %s
                ```
                Based on the above analysis please respond with structured JSON.
                """.formatted(plainTextAnalysis).trim();


        return ollamaApiService.callOllamaStructuredResponse(
                structuringMessage.formatted(ollamaApiService.buildExampleJson(TableOfContentsAnalysis.class)),
                userMessage, false, TableOfContentsAnalysis.class);

    }

    private String analyzeRetry(
            final String frontText,
            final String previousRegex,
            final String failureReason
    ) throws IOException, InterruptedException {
        final String systemPrompt = SYSTEM_PROMPT_RETRY.formatted(previousRegex, failureReason);

        final String userMessage = """
                ===== Book Text (beginning) =====
                %s
                =================================

                The previous regex did not work. Based on the text above, suggest a different Java regex \
                that matches all chapter headings.
                """.formatted(frontText).trim();

        return ollamaApiService.callOllamaPlainTextResponse(systemPrompt, userMessage, true);
    }

    private ChapterSplitterAIAnalysisResult analyzeStructured(
            final String plainTextAnalysis
    ) throws IOException, InterruptedException {
        final String userMessage = """
                ===== Chapter Pattern Analysis =====
                %s
                ====================================

                Based on the above analysis please respond with structured JSON.
                """.formatted(plainTextAnalysis).trim();

        try {
            return ollamaApiService.callOllamaStructuredResponse(
                    SYSTEM_PROMPT_STRUCTURED.formatted(ollamaApiService.buildExampleJson(ChapterSplitterAIAnalysisResult.class)),
                    userMessage, true, ChapterSplitterAIAnalysisResult.class);
        } catch (final Exception e) {
            if (isJsonEscapeError(e)) {
                LOG.warnf("(analyzeStructured) JSON parse failed (likely regex escape issue), " +
                        "falling back to plain-text extraction: %s", e.getMessage());
                return extractRegexFromPlainText(plainTextAnalysis);
            }
            throw e;
        }
    }

    private static boolean isJsonEscapeError(final Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof com.fasterxml.jackson.core.JsonParseException
                    && t.getMessage() != null
                    && t.getMessage().contains("Unrecognized character escape")) {
                return true;
            }
        }
        return false;
    }

    private static ChapterSplitterAIAnalysisResult extractRegexFromPlainText(final String plainTextAnalysis) {
        final Pattern splitRegexLabel = Pattern.compile(
                "(?i)split\\s+regex\\s*:\\s*\\n?(.+)", Pattern.MULTILINE);
        final Matcher m = splitRegexLabel.matcher(plainTextAnalysis);
        if (m.find()) {
            final String regex = m.group(1).trim();
            LOG.infof("(extractRegexFromPlainText) extracted regex from plain text: %s", regex);
            return new ChapterSplitterAIAnalysisResult(regex);
        }
        LOG.warn("(extractRegexFromPlainText) could not find 'Split Regex:' in plain text analysis");
        return new ChapterSplitterAIAnalysisResult("");
    }

    private record ValidationOutcome(boolean valid, int matchCount, String failureReason) {
        static ValidationOutcome success(final int matchCount) {
            return new ValidationOutcome(true, matchCount, null);
        }

        static ValidationOutcome failure(final int matchCount, final String reason) {
            return new ValidationOutcome(false, matchCount, reason);
        }
    }
}
