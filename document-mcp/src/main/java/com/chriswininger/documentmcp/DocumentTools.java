package com.chriswininger.documentmcp;

import com.chriswininger.documentmcp.client.DocumentApiClient;
import com.chriswininger.documentmcp.dto.DocumentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

public class DocumentTools {

    private static final Logger LOG = Logger.getLogger(DocumentTools.class);

    private final DocumentApiClient client;
    private final ObjectMapper objectMapper;

    public DocumentTools(@RestClient final DocumentApiClient client, final ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "List all ingested documents. Returns id, title, author, summary, and metadata for each document.")
    String listDocuments() throws JsonProcessingException {
        LOG.info("Tool call: listDocuments");
        final List<DocumentResponse> docs = client.listDocuments(false);
        return objectMapper.writeValueAsString(docs);
    }

    @Tool(description = "Get a specific document by its ID. Returns title, author, summary, characters, and metadata.")
    String getDocument(
            @ToolArg(description = "The document ID") final Long id,
            @ToolArg(description = "Whether to include the full text of the document") final boolean includeFullText
    ) throws JsonProcessingException {
        LOG.infof("Tool call: getDocument(id=%d, includeFullText=%b)", id, includeFullText);
        final DocumentResponse doc = client.getDocument(id, includeFullText);
        return objectMapper.writeValueAsString(doc);
    }
}
