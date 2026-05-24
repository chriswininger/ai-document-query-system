package com.chriswininger.documentmcp;

import com.chriswininger.documentmcp.client.DocumentApiClient;
import com.chriswininger.documentmcp.dto.BookMetadataResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

public class BookMetadataTools {

    private static final Logger LOG = Logger.getLogger(BookMetadataTools.class);

    private final DocumentApiClient client;
    private final ObjectMapper objectMapper;

    public BookMetadataTools(@RestClient final DocumentApiClient client, final ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Get book metadata (title, author, publisher, year, characters, summary) for a given document.")
    String getBookMetadataByDocument(
            @ToolArg(description = "The document ID to get book metadata for") final Long documentId
    ) throws JsonProcessingException {
        LOG.infof("Tool call: getBookMetadataByDocument(documentId=%d)", documentId);
        final BookMetadataResponse metadata = client.getBookMetadataByDocument(documentId, false);
        return objectMapper.writeValueAsString(metadata);
    }

    @Tool(description = "Get book metadata by its own ID. Returns title, author, publisher, year, characters, and summary.")
    String getBookMetadata(
            @ToolArg(description = "The book metadata ID") final Long id,
            @ToolArg(description = "Whether to include the full front/back text") final boolean includeFullText
    ) throws JsonProcessingException {
        LOG.infof("Tool call: getBookMetadata(id=%d, includeFullText=%b)", id, includeFullText);
        final BookMetadataResponse metadata = client.getBookMetadata(id, includeFullText);
        return objectMapper.writeValueAsString(metadata);
    }
}
