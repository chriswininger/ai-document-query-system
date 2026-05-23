package com.chriswininger.api.services;

import com.chriswininger.api.dto.BookMetadataAnalysis;
import com.chriswininger.api.dto.ChapterSummary;
import com.chriswininger.api.dto.ImportedBookResult;
import com.chriswininger.api.dto.Segment;
import com.chriswininger.api.dto.inferenceresults.BookMetadataAnalysisResult;
import com.chriswininger.api.dto.inferenceresults.BookSummaryResult;
import com.chriswininger.api.dto.inferenceresults.ChapterSummaryResult;
import com.chriswininger.api.dto.inferenceresults.SegmentSummaryResult;
import com.chriswininger.db.generated.Tables;
import com.chriswininger.db.generated.tables.records.BookMetadataRecord;
import com.chriswininger.db.generated.tables.records.ChaptersRecord;
import com.chriswininger.db.generated.tables.records.DocumentsRecord;
import com.chriswininger.db.generated.tables.records.SectionsRecord;
import dev.langchain4j.data.document.Metadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class ImportBookService {
    private static final Logger LOG = Logger.getLogger(ImportBookService.class);

    private final BookMetaExtractionService bookMetaExtractionService;
    private final ChapterService chapterService;
    private final BookSummaryService bookSummaryService;
    private final SegmentSummaryService segmentSummaryService;
    private final DSLContext dsl;
    private final VectorStoreService vectorStoreService;

    public ImportBookService(
            final BookMetaExtractionService bookMetaExtractionService,
            final ChapterService chapterService,
            final BookSummaryService bookSummaryService,
            final SegmentSummaryService segmentSummaryService,
            final DSLContext dsl,
            final VectorStoreService vectorStoreService
    ) {
        this.bookMetaExtractionService = bookMetaExtractionService;
        this.chapterService = chapterService;
        this.bookSummaryService = bookSummaryService;
        this.segmentSummaryService = segmentSummaryService;
        this.dsl = dsl;
        this.vectorStoreService = vectorStoreService;
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

        final String frontBackSummary = bookMetaDataSummary.bookMetadataAnalysisResult().summary();
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
                    i,
                    chapterSummaryResults.get(i),
                    summarizedChapters.get(i).label(),
                    summarizedChapters.get(i).content(),
                    segments
            ));
        }

        final var result = new ImportedBookResult(bookSummary, bookMetaDataSummary, chapterSummaries);

        persistImportedBook(result, bookContents);

        return result;
    }

    private void persistImportedBook(final ImportedBookResult result, final String bookContents) {
        final BookSummaryResult bookSummary = result.bookSummary();
        final BookMetadataAnalysis metaAnalysis = result.bookMetadataAnalysis();
        final BookMetadataAnalysisResult metaResult = metaAnalysis.bookMetadataAnalysisResult();

        final DocumentsRecord doc = dsl.newRecord(Tables.DOCUMENTS);
        doc.setTitle(bookSummary.title());
        doc.setType("book");
        doc.setSummary(bookSummary.summary());
        doc.setCharacters(toArray(bookSummary.characters()));
        doc.setFullText(bookContents);
        doc.setYearPublished(bookSummary.yearPublished() != null ? bookSummary.yearPublished().intValue() : null);
        doc.setAuthorName(bookSummary.authorName());
        doc.setPossibleQuestionsThisAnswers(toArray(bookSummary.possibleQuestionsThisAnswers()));
        doc.store();

        final Long documentId = doc.getId();
        LOG.infof("(persistImportedBook) inserted document id=%d", documentId);

        final BookMetadataRecord meta = dsl.newRecord(Tables.BOOK_METADATA);
        meta.setDocumentId(documentId);
        meta.setSummary(metaResult.summary());
        meta.setTitle(metaResult.title());
        meta.setAuthorName(metaResult.authorName());
        meta.setPublisher(metaResult.publisher());
        meta.setYearPublished(metaResult.yearPublished() != null ? metaResult.yearPublished().intValue() : null);
        meta.setCharacters(toArray(metaResult.characters()));
        meta.setPossibleQuestionsThisAnswers(toArray(metaResult.possibleQuestionsThisAnswers()));
        meta.setHasSummaryInformation(metaResult.hasSummaryInformation());
        meta.setFullTextFront(metaAnalysis.fullFrontText());
        meta.setFullTextBack(metaAnalysis.fullBackText());
        meta.store();

        LOG.infof("(persistImportedBook) inserted book_metadata id=%d", meta.getId());

        for (final ChapterSummary chapter : result.chapterSummaries()) {
            final ChapterSummaryResult chpResult = chapter.chapterSummaryResult();

            final ChaptersRecord chpRecord = dsl.newRecord(Tables.CHAPTERS);
            chpRecord.setDocumentId(documentId);
            chpRecord.setChapterTitle(chapter.chapterTitle());
            chpRecord.setSequence(chapter.sequence());
            chpRecord.setSummary(chpResult.summary());
            chpRecord.setCharacters(toArray(chpResult.characters()));
            chpRecord.setFullText(chapter.fullChapterText());
            chpRecord.setPossibleQuestionsThisAnswers(toArray(chpResult.possibleQuestionsThisAnswers()));
            chpRecord.store();

            final Long chapterId = chpRecord.getId();
            LOG.infof("(persistImportedBook) inserted chapter id=%d (sequence=%d)", chapterId, chapter.sequence());

            for (final Segment segment : chapter.segments()) {
                final SegmentSummaryResult segResult = segment.segmentSummary();

                final SectionsRecord secRecord = dsl.newRecord(Tables.SECTIONS);
                secRecord.setChapterId(chapterId);
                secRecord.setSequence(segment.sequence());
                secRecord.setSummary(segResult.summary());
                secRecord.setCharacters(toArray(segResult.characters()));
                secRecord.setFullText(segment.fullSegment());
                secRecord.setPossibleQuestionsThisAnswers(toArray(segResult.possibleQuestionsThisAnswers()));
                secRecord.store();

                final Metadata metadata = new Metadata()
                        .put("sectionId", secRecord.getId())
                        .put("chapterId", chapterId)
                        .put("documentId", documentId)
                        .put("bookTitle", bookSummary.title())
                        .put("chapterLabel", chapter.chapterTitle())
                        .put("characters", toJsonArray(segResult.characters()))
                        .put("possibleQuestionsThisAnswers", toJsonArray(segResult.possibleQuestionsThisAnswers()));

                vectorStoreService.storeVector(segment.fullSegment(), metadata);
            }

            LOG.infof("(persistImportedBook) inserted %d sections for chapter id=%d",
                    chapter.segments().size(), chapterId);
        }

        LOG.infof("(persistImportedBook) persistence complete for document id=%d", documentId);
    }

    private static String[] toArray(final List<String> list) {
        return list != null ? list.toArray(String[]::new) : null;
    }

    private static String toJsonArray(final List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        final String items = list.stream()
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(","));
        return "[" + items + "]";
    }
}
