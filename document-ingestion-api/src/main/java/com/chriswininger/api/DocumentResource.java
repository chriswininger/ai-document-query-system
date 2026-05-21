package com.chriswininger.api;

import com.chriswininger.api.dto.inferenceresults.ChapterSummary;
import com.chriswininger.api.dto.inferenceresults.BookMetadataAnalysisResult;
import com.chriswininger.api.dto.requests.SubmitDocumentRequest;
import com.chriswininger.api.services.BookMetaExtractionService;
import com.chriswininger.api.services.ChapterService;
import com.chriswininger.api.services.ChapterSummaryAiServiceDirect;
import com.chriswininger.api.services.SummarySearchService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Path(ApiConstants.BASE_REST_V1)
public class DocumentResource {

    private static final Logger LOG = Logger.getLogger(DocumentResource.class);

    private final ChapterService chapterService;

    private final BookMetaExtractionService bookMetaExtractionService;

    public DocumentResource(
            final ChapterService chapterService,
            final BookMetaExtractionService bookMetaExtractionService
    ) {
        this.chapterService = chapterService;
        this.bookMetaExtractionService = bookMetaExtractionService;
    }

    @POST
    @Path("/test/generate-test-chapters")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void generateTestChapters(SubmitDocumentRequest request) {
        final String documentText = request.document();
        final String documentTitle = request.documentTitle();

        LOG.infof("POST /rest/v1/test/generate-test-chapters — title", documentTitle);
        LOG.infof("POST /rest/v1/test/generate-test-chapters — document size: %d bytes", documentText.length());
        final Pattern chapterSplitPattern = getChapterSplitPattern(request);
        final var chapters = chapterService.splitIntoChapters(documentText, chapterSplitPattern);
        LOG.infof("POST /rest/v1/test/generate-test-chapters — num chapters", chapters.size());

        final String safeTitle = toSafeFileName(documentTitle);

        final var outDir = Paths.get("src/test/resources/testDocuments/" + safeTitle);
        try {
            Files.createDirectories(outDir);
        } catch (IOException e) {
            LOG.errorf("Failed to create output directory '%s': %s", outDir, e.getMessage());
            return;
        }
        chapters.forEach(chp -> {
            final String safeChapter = toSafeFileName(chp.label());

            String fileName = safeTitle + "_" + safeChapter + ".txt";
            try {
                LOG.infof("Saving chapter: '%s' (%s num characters)", fileName, chp.content().length());
                Files.writeString(outDir.resolve(fileName), chp.content());
                LOG.infof("Saved chapter: %s", fileName);
            } catch (IOException e) {
                LOG.errorf("Failed to write chapter file '%s': %s", fileName, e.getMessage());
            }
        });
    }

    @POST
    @Path("/submit-document")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BookMetadataAnalysisResult submitDocument(SubmitDocumentRequest request) throws IOException, InterruptedException {
        final String bookContents = request.document();
        LOG.infof("POST /rest/v1/submit-document — document size: %d bytes", bookContents.length());

        final Pattern chapterSplitPattern = getChapterSplitPattern(request);

        LOG.infof("POST /rest/v1/submit-document — Summarizing based on front and back of book");
        final var bookMetaDataSummary = bookMetaExtractionService.extractMetaDataFromTheBook(bookContents, chapterSplitPattern);
        LOG.infof("POST /rest/v1/submit-document — bookMetaDataSummary %s", bookMetaDataSummary);

        final var chapters = chapterService.splitIntoChapters(bookContents, chapterSplitPattern);
        LOG.infof("POST /rest/v1/submit-document — num chapters", chapters.size());

        final List<ChapterSummary> chapterSummaries = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            final long startTime = System.currentTimeMillis();
            if ("Intro".equals(chapters.get(i).label())) {
                // skip intro
                continue;
            }

            // TODO: We could include just a tiny bit of the previous chapter
            // that would cathc the preview chapter issue, we could move this loop
            // into the service
            LOG.infof("==== Start Summarizing Chapter: [%s] -> %s =====", i, chapters.get(i).label());
            final var chpSummary = chapterService.summarizeChapter(chapters.get(i));
            LOG.infof("Done Summarizing Chapter: %s -- %s -> took %s ms",
                    i, chapters.get(i).label(), System.currentTimeMillis() - startTime);
            LOG.infof("summary: '%s'", chpSummary);
            LOG.info("=============================");

            chapterSummaries.add(chpSummary);
        }

        return bookMetaDataSummary; // Response.accepted(new SubmitDocumentResponse("success", summary)).build();
    }

    private String toSafeFileName(String input) {
        return input
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .toLowerCase();
    }

    private Pattern getChapterSplitPattern(SubmitDocumentRequest submitDocumentRequest) {
        final var pattern = submitDocumentRequest.chapterSplitPattern();

        if (Objects.isNull(pattern)) {
            return  null;
        } else {
            LOG.infof("Using chapterSplitPattern: %s", pattern);
            return Pattern.compile(pattern);
        }
    }
}
