package com.chriswininger.api.documents.services;

import com.chriswininger.api.dto.inferenceresults.ChapterSplitterAIAnalysisResult;
import com.chriswininger.api.dto.inferenceresults.ChapterSplitterResult;
import com.chriswininger.api.dto.inferenceresults.ChapterSummaryResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class ChapterService {
    private static final Logger LOG = Logger.getLogger(ChapterService.class);

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
        // TODO: We should clean this up we need to:
        //     Determine table of contents even when a splitter is provided
        //     Add the tabel of contents as a chapter
        //     That probably means invoking our contents detection here instead of inside our split function
        //       and getting rid of the test that exists on the ChapterSplitterAIAnalysisResultTest involving
        //       contents
        if (Objects.isNull(splitPattern)) {
            LOG.infof("(splitIntoChapters) no split pattern provided, using AI detection");

            try {
                final ChapterSplitterResult result = chapterSplitAIService.detectSplitExpression(document);
                LOG.infof("(detectSplitPattern) AI detected split expression: %s",
                        result.aiAnalysisResult().splitExpression());

                final var aiSplitPattern = Pattern.compile(result.aiAnalysisResult().splitExpression());

                if (result.tableOfContentsAnalysis().containsTableOfContents()) {
                    return splitIntoChapters(document.replace(result.tableOfContentsAnalysis().tableOfConents(), ""), aiSplitPattern);
                } else {
                    return splitIntoChapters(document, aiSplitPattern);
                }
            } catch (final IOException | InterruptedException e) {
                throw new RuntimeException("Failed to detect chapter split pattern via AI", e);
            }
        }

        final Matcher matcher = splitPattern.matcher(document);
        List<Chapter> chapters = new ArrayList<>();

        int lastEnd = 0;
        String lastHeader = null;
        final Set<String> existingLabels = new HashSet<>();
        int labelPostFixNdx = 0;
        while (matcher.find()) {
            // text between the previous match and this one becomes the body of the last chapter
            final String body = document.substring(lastEnd, matcher.start());
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
            lastHeader = matcher.group(); // the matched header itself
            lastEnd = matcher.end();
        }

        if (lastHeader == null) {
            LOG.warn("Chapter splitter found 0 chapters");
            throw new IllegalArgumentException(
                    "Chapter split pattern '%s' did not match anything in the document"
                            .formatted(splitPattern.pattern()));
        }

        String label = lastHeader.trim();

        // add a postfix if we've seen this before, this happening, for example, when a book
        // contains the first chapter of the next book in a series as preview
        if (existingLabels.contains(label)) {
            label += ("_" + (++labelPostFixNdx));
        }
        // tail — everything after the last header
        chapters.add(new Chapter(label, document.substring(lastEnd)));

        return chapters;
    }

//    private Pattern detectSplitPattern(final String document) {
//        try {
//            final ChapterSplitterResult result = chapterSplitAIService.detectSplitExpression(document);
//            LOG.infof("(detectSplitPattern) AI detected split expression: %s",
//                    result.aiAnalysisResult().splitExpression());
//            return Pattern.compile(result.aiAnalysisResult().splitExpression());
//        } catch (final IOException | InterruptedException e) {
//            throw new RuntimeException("Failed to detect chapter split pattern via AI", e);
//        }
//    }
}
