package com.chriswininger.api;

import com.chriswininger.api.services.ChapterService;
import com.chriswininger.api.services.SummarySearchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path(ApiConstants.BASE_REST_V1)
public class DocumentResource {

    private static final Logger LOG = Logger.getLogger(DocumentResource.class);

    @Inject
    SummarySearchService summarySearchService;

    @Inject
    ChapterService chapterService;

    record SubmitDocumentResponse(String status, String summary) {}

    @POST
    @Path("/submit-document")
    @Consumes({MediaType.TEXT_PLAIN, "text/markdown"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response submitDocument(String body) {
        LOG.infof("POST /rest/v1/submit-document hit — document size: %d bytes", body.length());
        String summary = summarySearchService.findSummaries(body);
        final var test = chapterService.splitIntoChapters(body);

        LOG.infof("!!! MUCH chp %s", test);
        return Response.accepted(new SubmitDocumentResponse("success", summary)).build();
    }
}
