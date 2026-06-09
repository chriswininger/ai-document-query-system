package com.chriswininger.api.documents;

import com.chriswininger.api.ApiConstants;
import com.chriswininger.api.documents.dto.requests.ChapterResponse;
import com.chriswininger.db.generated.Tables;
import com.chriswininger.db.generated.tables.records.ChaptersRecord;
import org.eclipse.microprofile.openapi.annotations.Operation;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.util.Arrays;
import java.util.List;

@Path(ApiConstants.BASE_REST_V1 + "/chapters")
public class ChapterResource {

    private static final Logger LOG = Logger.getLogger(ChapterResource.class);

    private final DSLContext dsl;

    public ChapterResource(final DSLContext dsl) {
        this.dsl = dsl;
    }

    @GET
    @Path("/by-document/{documentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "listChaptersByDocument")
    public List<ChapterResponse> listByDocument(
            @PathParam("documentId") final Long documentId,
            @QueryParam("include_full_text") @DefaultValue("false") final boolean includeFullText
    ) {
        LOG.infof("GET /chapters/by-document/%d (include_full_text=%b)", documentId, includeFullText);

        return dsl.selectFrom(Tables.CHAPTERS)
                .where(Tables.CHAPTERS.DOCUMENT_ID.eq(documentId))
                .fetch()
                .map(record -> toResponse(record, includeFullText));
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getChapterById")
    public ChapterResponse getById(
            @PathParam("id") final Long id,
            @QueryParam("include_full_text") @DefaultValue("false") final boolean includeFullText
    ) {
        LOG.infof("GET /chapters/%d (include_full_text=%b)", id, includeFullText);

        final ChaptersRecord record = dsl.selectFrom(Tables.CHAPTERS)
                .where(Tables.CHAPTERS.ID.eq(id))
                .fetchOne();

        if (record == null) {
            throw new NotFoundException("Chapter not found: " + id);
        }

        return toResponse(record, includeFullText);
    }

    private static ChapterResponse toResponse(final ChaptersRecord record, final boolean includeFullText) {
        return new ChapterResponse(
                record.getId(),
                record.getDocumentId(),
                record.getChapterTitle(),
                record.getSequence(),
                record.getSummary(),
                record.getCharacters() != null ? Arrays.asList(record.getCharacters()) : List.of(),
                includeFullText ? record.getFullText() : null,
                record.getPossibleQuestionsThisAnswers() != null
                        ? Arrays.asList(record.getPossibleQuestionsThisAnswers())
                        : List.of(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
