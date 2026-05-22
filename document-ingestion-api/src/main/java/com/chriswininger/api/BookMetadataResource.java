package com.chriswininger.api;

import com.chriswininger.api.dto.requests.BookMetadataResponse;
import com.chriswininger.db.generated.Tables;
import com.chriswininger.db.generated.tables.records.BookMetadataRecord;
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

@Path(ApiConstants.BASE_REST_V1 + "/book-metadata")
public class BookMetadataResource {

    private static final Logger LOG = Logger.getLogger(BookMetadataResource.class);

    private final DSLContext dsl;

    public BookMetadataResource(final DSLContext dsl) {
        this.dsl = dsl;
    }

    @GET
    @Path("/by-document/{documentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public BookMetadataResponse getByDocument(
            @PathParam("documentId") final Long documentId,
            @QueryParam("include_full_text") @DefaultValue("false") final boolean includeFullText
    ) {
        LOG.infof("GET /book-metadata/by-document/%d (include_full_text=%b)", documentId, includeFullText);

        final BookMetadataRecord record = dsl.selectFrom(Tables.BOOK_METADATA)
                .where(Tables.BOOK_METADATA.DOCUMENT_ID.eq(documentId))
                .fetchOne();

        if (record == null) {
            throw new NotFoundException("Book metadata not found for document: " + documentId);
        }

        return toResponse(record, includeFullText);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BookMetadataResponse getById(
            @PathParam("id") final Long id,
            @QueryParam("include_full_text") @DefaultValue("false") final boolean includeFullText
    ) {
        LOG.infof("GET /book-metadata/%d (include_full_text=%b)", id, includeFullText);

        final BookMetadataRecord record = dsl.selectFrom(Tables.BOOK_METADATA)
                .where(Tables.BOOK_METADATA.ID.eq(id))
                .fetchOne();

        if (record == null) {
            throw new NotFoundException("Book metadata not found: " + id);
        }

        return toResponse(record, includeFullText);
    }

    private static BookMetadataResponse toResponse(final BookMetadataRecord record, final boolean includeFullText) {
        return new BookMetadataResponse(
                record.getId(),
                record.getDocumentId(),
                record.getSummary(),
                record.getTitle(),
                record.getAuthorName(),
                record.getPublisher(),
                record.getYearPublished(),
                record.getCharacters() != null ? Arrays.asList(record.getCharacters()) : List.of(),
                record.getPossibleQuestionsThisAnswers() != null
                        ? Arrays.asList(record.getPossibleQuestionsThisAnswers())
                        : List.of(),
                record.getHasSummaryInformation(),
                includeFullText ? record.getFullTextFront() : null,
                includeFullText ? record.getFullTextBack() : null,
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
