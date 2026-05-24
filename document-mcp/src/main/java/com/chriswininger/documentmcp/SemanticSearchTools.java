package com.chriswininger.documentmcp;

import com.chriswininger.documentmcp.client.DocumentApiClient;
import com.chriswininger.documentmcp.dto.SemanticSearchMatchResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

public class SemanticSearchTools {

    private static final Logger LOG = Logger.getLogger(SemanticSearchTools.class);

    private final DocumentApiClient client;
    private final ObjectMapper objectMapper;

    public SemanticSearchTools(@RestClient final DocumentApiClient client, final ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Search documents using semantic similarity. Finds text sections that are semantically related to the search phrase. Optionally filter by document or chapter.")
    String semanticSearch(
            @ToolArg(description = "The search phrase to find semantically similar content for") final String phrase,
            @ToolArg(description = "Optional document ID to restrict search to a specific document") final Long documentId,
            @ToolArg(description = "Optional chapter ID to restrict search to a specific chapter") final Long chapterId,
            @ToolArg(description = "Maximum number of results to return (default 10)") final int maxResults
    ) throws JsonProcessingException {
        LOG.infof("Tool call: semanticSearch(phrase='%s', documentId=%s, chapterId=%s, maxResults=%d)",
                phrase, documentId, chapterId, maxResults);
        final List<SemanticSearchMatchResponse> matches = client.semanticSearch(phrase, documentId, chapterId, maxResults);
        return objectMapper.writeValueAsString(matches);
    }
}
