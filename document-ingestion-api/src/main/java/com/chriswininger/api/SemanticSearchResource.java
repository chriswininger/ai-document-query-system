package com.chriswininger.api;

import com.chriswininger.api.dto.requests.SemanticSearchMatchResponse;
import com.chriswininger.api.services.VectorStoreService;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.util.List;

@Path(ApiConstants.BASE_REST_V1 + "/semantic-search")
public class SemanticSearchResource {

    private static final Logger LOG = Logger.getLogger(SemanticSearchResource.class);

    private final VectorStoreService vectorStoreService;

    public SemanticSearchResource(final VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<SemanticSearchMatchResponse> search(
            @QueryParam("phrase") final String phrase,
            @QueryParam("documentId") final Long documentId,
            @QueryParam("chapterId") final Long chapterId,
            @QueryParam("maxResults") @DefaultValue("10") final int maxResults
    ) {
        if (phrase == null || phrase.isBlank()) {
            throw new BadRequestException("query param 'phrase' is required");
        }

        LOG.infof(
                "GET /semantic-search (documentId=%s, chapterId=%s, maxResults=%d)",
                documentId,
                chapterId,
                maxResults
        );

        return vectorStoreService.search(phrase, documentId, chapterId, maxResults);
    }
}
