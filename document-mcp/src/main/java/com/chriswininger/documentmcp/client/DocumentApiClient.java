package com.chriswininger.documentmcp.client;

import com.chriswininger.documentmcp.dto.BookMetadataResponse;
import com.chriswininger.documentmcp.dto.ChapterResponse;
import com.chriswininger.documentmcp.dto.DocumentResponse;
import com.chriswininger.documentmcp.dto.PagedResponse;
import com.chriswininger.documentmcp.dto.SectionResponse;
import com.chriswininger.documentmcp.dto.SemanticSearchMatchResponse;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "document-api")
@Path("rest/v1")
@Produces(MediaType.APPLICATION_JSON)
public interface DocumentApiClient {

    // Documents

    @GET
    @Path("/documents")
    List<DocumentResponse> listDocuments(
            @QueryParam("include_full_text") @DefaultValue("false") boolean includeFullText
    );

    @GET
    @Path("/documents/{id}")
    DocumentResponse getDocument(
            @PathParam("id") Long id,
            @QueryParam("include_full_text") @DefaultValue("false") boolean includeFullText
    );

    // Chapters

    @GET
    @Path("/chapters/by-document/{documentId}")
    List<ChapterResponse> listChaptersByDocument(
            @PathParam("documentId") Long documentId,
            @QueryParam("include_full_text") @DefaultValue("false") boolean includeFullText
    );

    @GET
    @Path("/chapters/{id}")
    ChapterResponse getChapter(
            @PathParam("id") Long id,
            @QueryParam("include_full_text") @DefaultValue("false") boolean includeFullText
    );

    // Sections

    @GET
    @Path("/sections/by-chapter/{chapterId}")
    List<SectionResponse> listSectionsByChapter(
            @PathParam("chapterId") Long chapterId,
            @QueryParam("include_full_text") @DefaultValue("false") boolean includeFullText
    );

    @GET
    @Path("/sections/by-document/{documentId}")
    PagedResponse<SectionResponse> listSectionsByDocument(
            @PathParam("documentId") Long documentId,
            @QueryParam("include_full_text") @DefaultValue("false") boolean includeFullText,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("page_size") @DefaultValue("20") int pageSize
    );

    @GET
    @Path("/sections/by-sequence/{chapterId}/{sequenceNumber}")
    SectionResponse getSectionBySequence(
            @PathParam("chapterId") Long chapterId,
            @PathParam("sequenceNumber") Integer sequenceNumber,
            @QueryParam("include_full_text") @DefaultValue("false") boolean includeFullText
    );

    @GET
    @Path("/sections/{id}")
    SectionResponse getSection(
            @PathParam("id") Long id,
            @QueryParam("include_full_text") @DefaultValue("false") boolean includeFullText
    );

    // Book Metadata

    @GET
    @Path("/book-metadata/by-document/{documentId}")
    BookMetadataResponse getBookMetadataByDocument(
            @PathParam("documentId") Long documentId,
            @QueryParam("include_full_text") @DefaultValue("false") boolean includeFullText
    );

    @GET
    @Path("/book-metadata/{id}")
    BookMetadataResponse getBookMetadata(
            @PathParam("id") Long id,
            @QueryParam("include_full_text") @DefaultValue("false") boolean includeFullText
    );

    // Semantic Search

    @GET
    @Path("/semantic-search")
    List<SemanticSearchMatchResponse> semanticSearch(
            @QueryParam("phrase") String phrase,
            @QueryParam("documentId") Long documentId,
            @QueryParam("chapterId") Long chapterId,
            @QueryParam("maxResults") @DefaultValue("10") int maxResults
    );
}
