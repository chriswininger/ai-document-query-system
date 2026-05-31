package com.chriswininger.api.documents.services;

import com.chriswininger.api.dto.inferenceresults.ChapterSummaryResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class ChapterService {

    private final ChapterSummaryAiServiceDirect chapterSummaryAiService;

    public ChapterService(final ChapterSummaryAiServiceDirect chapterSummaryAiService) {
        this.chapterSummaryAiService = chapterSummaryAiService;
    }

    public ChapterSummaryResult summarizeChapter(final Chapter chapter) {
        return chapterSummaryAiService.summarize(chapter.label(), chapter.content());
    }

    public List<Chapter> splitIntoChapters(final String document, final Pattern splitPattern) {
        if (Objects.isNull(splitPattern)) {
            return List.of();
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
}
