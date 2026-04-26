package com.chriswininger.api;

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

    record SubmitDocumentResponse(String status) {}

    @POST
    @Path("/submit-document")
    @Consumes({MediaType.TEXT_PLAIN, "text/markdown"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response submitDocument(String body) {
        LOG.infof("POST /rest/v1/submit-document hit — document size: %d bytes", body.length());
        return Response.accepted(new SubmitDocumentResponse("success")).build();
    }
}
