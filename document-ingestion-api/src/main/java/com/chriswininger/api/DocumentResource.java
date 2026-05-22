package com.chriswininger.api;

import com.chriswininger.api.dto.ImportedBookResult;
import com.chriswininger.api.dto.requests.DocumentResponse;
import com.chriswininger.api.dto.requests.SubmitDocumentRequest;
import com.chriswininger.api.services.ChapterService;
import com.chriswininger.api.services.ImportBookService;
import com.chriswininger.db.generated.Tables;
import com.chriswininger.db.generated.tables.records.DocumentsRecord;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Path(ApiConstants.BASE_REST_V1)
public class DocumentResource {

    private static final Logger LOG = Logger.getLogger(DocumentResource.class);

    private final ChapterService chapterService;

    private final ImportBookService importBookService;

    private final DSLContext dsl;

    public DocumentResource(
            final ChapterService chapterService,
            final ImportBookService importBookService,
            final DSLContext dsl
    ) {
        this.chapterService = chapterService;
        this.importBookService = importBookService;
        this.dsl = dsl;
    }

    @GET
    @Path("/documents")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DocumentResponse> listDocuments(
            @QueryParam("include_full_text") @DefaultValue("false") final boolean includeFullText
    ) {
        LOG.infof("GET /documents (include_full_text=%b)", includeFullText);

        return dsl.selectFrom(Tables.DOCUMENTS)
                .fetch()
                .map(record -> toDocumentResponse(record, includeFullText));
    }

    @GET
    @Path("/documents/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DocumentResponse getDocumentById(
            @PathParam("id") final Long id,
            @QueryParam("include_full_text") @DefaultValue("false") final boolean includeFullText
    ) {
        LOG.infof("GET /documents/%d (include_full_text=%b)", id, includeFullText);

        final DocumentsRecord record = dsl.selectFrom(Tables.DOCUMENTS)
                .where(Tables.DOCUMENTS.ID.eq(id))
                .fetchOne();

        if (record == null) {
            throw new NotFoundException("Document not found: " + id);
        }

        return toDocumentResponse(record, includeFullText);
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

    // TODO: We should include a chapter title in our chapter table and also an index, to let us
    // track sequence
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

    private static DocumentResponse toDocumentResponse(final DocumentsRecord record, final boolean includeFullText) {
        return new DocumentResponse(
                record.getId(),
                record.getTitle(),
                record.getType(),
                record.getSummary(),
                record.getCharacters() != null ? Arrays.asList(record.getCharacters()) : List.of(),
                includeFullText ? record.getFullText() : null,
                record.getYearPublished(),
                record.getAuthorName(),
                record.getPossibleQuestionsThisAnswers() != null
                        ? Arrays.asList(record.getPossibleQuestionsThisAnswers())
                        : List.of(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
