package com.chriswininger.api.services;

import com.chriswininger.api.dto.ChapterSummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class ChapterService {

    @Inject
    ChapterSummaryAiService chapterSummaryAiService;

    public ChapterSummary summarizeChapter(final Chapter chapter) {
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
        while (matcher.find()) {
            // text between the previous match and this one becomes the body of the last chapter
            final String body = document.substring(lastEnd, matcher.start());
            if (lastHeader != null || !body.isBlank()) {
                chapters.add(new Chapter(Objects.nonNull(lastHeader) ? lastHeader.trim() : "Intro", body));
            }
            lastHeader = matcher.group(); // the matched header itself
            lastEnd = matcher.end();
        }

        // tail — everything after the last header
        chapters.add(new Chapter(lastHeader, document.substring(lastEnd)));

        return chapters;
    }
}
