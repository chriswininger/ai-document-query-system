package com.chriswininger.api;

import com.chriswininger.api.dto.ImportedBookResult;
import com.chriswininger.api.dto.requests.SubmitDocumentRequest;
import com.chriswininger.api.services.ChapterService;
import com.chriswininger.api.services.ImportBookService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.Objects;
import java.util.regex.Pattern;

@Path(ApiConstants.BASE_REST_V1)
public class DocumentResource {

    private static final Logger LOG = Logger.getLogger(DocumentResource.class);

    private final ChapterService chapterService;

    private final ImportBookService importBookService;

    public DocumentResource(
            final ChapterService chapterService,
            final ImportBookService importBookService
    ) {
        this.chapterService = chapterService;
        this.importBookService = importBookService;
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
    public ImportedBookResult submitDocument(SubmitDocumentRequest request) throws IOException, InterruptedException {
        final String bookContents = request.document();
        LOG.infof("POST /rest/v1/submit-document — document size: %d bytes", bookContents.length());

        final Pattern chapterSplitPattern = getChapterSplitPattern(request);

        return importBookService.importBook(bookContents, chapterSplitPattern);
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
