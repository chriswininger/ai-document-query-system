package com.chriswininger.api.services;

import com.chriswininger.api.dto.ChapterSummary;
import com.chriswininger.api.dto.ImportedBookResult;
import com.chriswininger.api.dto.Segment;
import com.chriswininger.api.dto.inferenceresults.ChapterSummaryResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class ImportBookService {
    private static final Logger LOG = Logger.getLogger(ImportBookService.class);

    private final BookMetaExtractionService bookMetaExtractionService;
    private final ChapterService chapterService;
    private final BookSummaryService bookSummaryService;
    private final SegmentSummaryService segmentSummaryService;

    public ImportBookService(
            final BookMetaExtractionService bookMetaExtractionService,
            final ChapterService chapterService,
            final BookSummaryService bookSummaryService,
            final SegmentSummaryService segmentSummaryService
    ) {
        this.bookMetaExtractionService = bookMetaExtractionService;
        this.chapterService = chapterService;
        this.bookSummaryService = bookSummaryService;
        this.segmentSummaryService = segmentSummaryService;
    }

    public ImportedBookResult importBook(
            final String bookContents,
            final Pattern chapterSplitPattern
    ) throws IOException, InterruptedException {
        LOG.infof("(importBook) Summarizing based on front and back of book");
        final var bookMetaDataSummary = bookMetaExtractionService.extractMetaDataFromTheBook(bookContents, chapterSplitPattern);
        LOG.infof("(importBook) bookMetaDataSummary %s", bookMetaDataSummary);

        final var chapters = chapterService.splitIntoChapters(bookContents, chapterSplitPattern);
        LOG.infof("(importBook) num chapters: %d", chapters.size());

        final List<Chapter> summarizedChapters = new ArrayList<>();
        final List<ChapterSummaryResult> chapterSummaryResults = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            final long startTime = System.currentTimeMillis();
            if ("Intro".equals(chapters.get(i).label())) {
                continue;
            }

            LOG.infof("==== Start Summarizing Chapter: [%s] -> %s =====", i, chapters.get(i).label());
            final var chpSummary = chapterService.summarizeChapter(chapters.get(i));
            LOG.infof("Done Summarizing Chapter: %s -- %s -> took %s ms",
                    i, chapters.get(i).label(), System.currentTimeMillis() - startTime);
            LOG.infof("summary: '%s'", chpSummary);
            LOG.info("=============================");

            summarizedChapters.add(chapters.get(i));
            chapterSummaryResults.add(chpSummary);
        }

        final String frontBackSummary = bookMetaDataSummary.summary();
        final List<String> chapterSummaryTexts = chapterSummaryResults.stream()
                .map(ChapterSummaryResult::summary)
                .toList();

        final var bookSummary = bookSummaryService.summarizeBook(frontBackSummary, chapterSummaryTexts);

        final List<ChapterSummary> chapterSummaries = new ArrayList<>();
        for (int i = 0; i < summarizedChapters.size(); i++) {
            LOG.infof("==== Start Segment Summarization: chapter %d -> %s =====",
                    i, summarizedChapters.get(i).label());
            final List<Segment> segments = segmentSummaryService.summarizeSegments(
                    summarizedChapters.get(i).content(), chapterSummaryResults.get(i), bookSummary);
            LOG.infof("Done Segment Summarization: chapter %d -> %d segments", i, segments.size());

            chapterSummaries.add(new ChapterSummary(
                    chapterSummaryResults.get(i),
                    summarizedChapters.get(i).content(),
                    segments
            ));
        }

        return new ImportedBookResult(bookSummary, bookMetaDataSummary, chapterSummaries);
    }
}
