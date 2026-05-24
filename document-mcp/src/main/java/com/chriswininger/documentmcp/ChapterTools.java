package com.chriswininger.documentmcp;

import com.chriswininger.documentmcp.client.DocumentApiClient;
import com.chriswininger.documentmcp.dto.ChapterResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

public class ChapterTools {

    private static final Logger LOG = Logger.getLogger(ChapterTools.class);

    private final DocumentApiClient client;
    private final ObjectMapper objectMapper;

    public ChapterTools(@RestClient final DocumentApiClient client, final ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "List all chapters for a given document. Returns chapter title, sequence, summary, and characters for each chapter.")
    String listChaptersByDocument(
            @ToolArg(description = "The document ID to list chapters for") final Long documentId
    ) throws JsonProcessingException {
        LOG.infof("Tool call: listChaptersByDocument(documentId=%d)", documentId);
        final List<ChapterResponse> chapters = client.listChaptersByDocument(documentId, false);
        return objectMapper.writeValueAsString(chapters);
    }

    @Tool(description = "Get a specific chapter by its ID. Returns title, sequence, summary, characters, and optionally full text.")
    String getChapter(
            @ToolArg(description = "The chapter ID") final Long id,
            @ToolArg(description = "Whether to include the full text of the chapter") final boolean includeFullText
    ) throws JsonProcessingException {
        LOG.infof("Tool call: getChapter(id=%d, includeFullText=%b)", id, includeFullText);
        final ChapterResponse chapter = client.getChapter(id, includeFullText);
        return objectMapper.writeValueAsString(chapter);
    }
}
