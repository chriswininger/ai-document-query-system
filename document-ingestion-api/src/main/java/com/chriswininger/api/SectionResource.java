package com.chriswininger.api;

import com.chriswininger.api.dto.requests.PagedResponse;
import com.chriswininger.api.dto.requests.SectionResponse;
import com.chriswininger.db.generated.Tables;
import com.chriswininger.db.generated.tables.records.SectionsRecord;
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

@Path(ApiConstants.BASE_REST_V1 + "/sections")
public class SectionResource {

    private static final Logger LOG = Logger.getLogger(SectionResource.class);

    private final DSLContext dsl;

    public SectionResource(final DSLContext dsl) {
        this.dsl = dsl;
    }

    @GET
    @Path("/by-chapter/{chapterId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SectionResponse> listByChapter(
            @PathParam("chapterId") final Long chapterId,
            @QueryParam("include_full_text") @DefaultValue("false") final boolean includeFullText
    ) {
        LOG.infof("GET /sections/by-chapter/%d (include_full_text=%b)", chapterId, includeFullText);

        return dsl.selectFrom(Tables.SECTIONS)
                .where(Tables.SECTIONS.CHAPTER_ID.eq(chapterId))
                .orderBy(Tables.SECTIONS.SEQUENCE.asc())
                .fetch()
                .map(record -> toResponse(record, includeFullText));
    }

    @GET
    @Path("/by-document/{documentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public PagedResponse<SectionResponse> listByDocument(
            @PathParam("documentId") final Long documentId,
            @QueryParam("include_full_text") @DefaultValue("false") final boolean includeFullText,
            @QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("page_size") @DefaultValue("20") final int pageSize
    ) {
        LOG.infof("GET /sections/by-document/%d page=%d pageSize=%d", documentId, page, pageSize);

        final int totalCount = dsl.selectCount()
                .from(Tables.SECTIONS)
                .join(Tables.CHAPTERS).on(Tables.SECTIONS.CHAPTER_ID.eq(Tables.CHAPTERS.ID))
                .where(Tables.CHAPTERS.DOCUMENT_ID.eq(documentId))
                .fetchOne(0, Integer.class);

        final int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        final List<SectionResponse> items = dsl.select(Tables.SECTIONS.fields())
                .from(Tables.SECTIONS)
                .join(Tables.CHAPTERS).on(Tables.SECTIONS.CHAPTER_ID.eq(Tables.CHAPTERS.ID))
                .where(Tables.CHAPTERS.DOCUMENT_ID.eq(documentId))
                .orderBy(Tables.CHAPTERS.SEQUENCE.asc(), Tables.SECTIONS.SEQUENCE.asc())
                .limit(pageSize)
                .offset((long) page * pageSize)
                .fetchInto(SectionsRecord.class)
                .stream()
                .map(r -> toResponse(r, includeFullText))
                .toList();

        return new PagedResponse<>(
                items,
                totalCount,
                page,
                pageSize,
                totalPages,
                page < totalPages - 1,
                page > 0
        );
    }

    @GET
    @Path("/by-sequence/{chapterId}/{sequenceNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public SectionResponse getBySequence(
            @PathParam("chapterId") final Long chapterId,
            @PathParam("sequenceNumber") final Integer sequenceNumber,
            @QueryParam("include_full_text") @DefaultValue("false") final boolean includeFullText
    ) {
        LOG.infof("GET /sections/by-sequence/%d/%d (include_full_text=%b)", chapterId, sequenceNumber, includeFullText);

        final SectionsRecord record = dsl.selectFrom(Tables.SECTIONS)
                .where(Tables.SECTIONS.CHAPTER_ID.eq(chapterId)
                        .and(Tables.SECTIONS.SEQUENCE.eq(sequenceNumber)))
                .fetchOne();

        if (record == null) {
            throw new NotFoundException("Section not found for chapterId=" + chapterId + ", sequence=" + sequenceNumber);
        }

        return toResponse(record, includeFullText);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public SectionResponse getById(
            @PathParam("id") final Long id,
            @QueryParam("include_full_text") @DefaultValue("false") final boolean includeFullText
    ) {
        LOG.infof("GET /sections/%d (include_full_text=%b)", id, includeFullText);

        final SectionsRecord record = dsl.selectFrom(Tables.SECTIONS)
                .where(Tables.SECTIONS.ID.eq(id))
                .fetchOne();

        if (record == null) {
            throw new NotFoundException("Section not found: " + id);
        }

        return toResponse(record, includeFullText);
    }

    private static SectionResponse toResponse(final SectionsRecord record, final boolean includeFullText) {
        return new SectionResponse(
                record.getId(),
                record.getChapterId(),
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
