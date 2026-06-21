package com.chriswininger.api.documents.services;

import com.chriswininger.api.dto.inferenceresults.ChapterSplitterAIAnalysisResult;
import com.chriswininger.api.dto.inferenceresults.ChapterSummaryResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

@ApplicationScoped
public class ChapterService {
    private static final Logger LOG = Logger.getLogger(ChapterService.class);

    private static final int TOC_DETECTION_SAMPLE_SIZE = 16000;

    private final ChapterSummaryAiServiceDirect chapterSummaryAiService;
    private final ChapterSplitAIService chapterSplitAIService;

    public ChapterService(
            final ChapterSummaryAiServiceDirect chapterSummaryAiService,
            final ChapterSplitAIService chapterSplitAIService
    ) {
        this.chapterSummaryAiService = chapterSummaryAiService;
        this.chapterSplitAIService = chapterSplitAIService;
    }

    public ChapterSummaryResult summarizeChapter(final Chapter chapter) {
        return chapterSummaryAiService.summarize(chapter.label(), chapter.content());
    }

    public List<Chapter> splitIntoChapters(final String document, final Pattern splitPattern) {
        String cleanedDocument = stripTableOfContents(document);

        final Pattern effectivePattern = isNull(splitPattern)
                ? detectSplitPattern(cleanedDocument)
                : splitPattern;

        final Matcher matcher = effectivePattern.matcher(cleanedDocument);
        List<Chapter> chapters = new ArrayList<>();

        int lastEnd = 0;
        String lastHeader = null;
        final Set<String> existingLabels = new HashSet<>();
        int labelPostFixNdx = 0;
        while (matcher.find()) {
            final String body = cleanedDocument.substring(lastEnd, matcher.start());
            if (lastHeader != null || !body.isBlank()) {
                // add a postfix if we've seen this before, this happening, for example, when a book
                // contains the first chapter of the next book in a series as preview
                String label = Objects.nonNull(lastHeader) ? lastHeader.trim() : "Intro";
                if (existingLabels.contains(label)) {
                    label += ("_" + (++labelPostFixNdx));
                }

                existingLabels.add(label);
                chapters.add(new Chapter(label, body));
            }
            lastHeader = matcher.group();
            lastEnd = matcher.end();
        }

        if (lastHeader == null) {
            LOG.warn("Chapter splitter found 0 chapters");
            throw new IllegalArgumentException(
                    "Chapter split pattern '%s' did not match anything in the document"
                            .formatted(effectivePattern.pattern()));
        }

        String label = lastHeader.trim();

        // add a postfix if we've seen this before, this happening, for example, when a book
        // contains the first chapter of the next book in a series as preview
        if (existingLabels.contains(label)) {
            label += ("_" + (++labelPostFixNdx));
        }
        chapters.add(new Chapter(label, cleanedDocument.substring(lastEnd)));

        return chapters;
    }

    private String stripTableOfContents(final String document) {
        try {
            final String sample = document.substring(0,
                    Math.min(document.length(), TOC_DETECTION_SAMPLE_SIZE));
            final var tocResult = chapterSplitAIService.findTableOfContents(sample);
            if (tocResult.containsTableOfContents()) {
                final int before = document.length();
                String cleaned = document.replace(tocResult.tableOfConents(), "");

                if (before == cleaned.length()) {
                    LOG.infof("(stripTableOfContents) Contents replace did not work.");
                }

                LOG.infof("(stripTableOfContents) removed TOC. Before: %d chars, After: %d chars",
                        before, cleaned.length());
                return cleaned;
            }
        } catch (final IOException | InterruptedException e) {
            LOG.warn("(stripTableOfContents) TOC detection failed, proceeding without stripping", e);
        }
        return document;
    }

    private Pattern detectSplitPattern(final String document) {
        try {
            final ChapterSplitterAIAnalysisResult result =
                    chapterSplitAIService.detectSplitExpression(document);
            LOG.infof("(detectSplitPattern) AI detected split expression: %s",
                    result.splitExpression());
            return Pattern.compile(result.splitExpression());
        } catch (final IOException | InterruptedException e) {
            throw new RuntimeException("Failed to detect chapter split pattern via AI", e);
        }
    }
}
