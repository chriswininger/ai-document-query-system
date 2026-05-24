package com.chriswininger.documentmcp;

import com.chriswininger.documentmcp.client.DocumentApiClient;
import com.chriswininger.documentmcp.dto.PagedResponse;
import com.chriswininger.documentmcp.dto.SectionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

public class SectionTools {

    private static final Logger LOG = Logger.getLogger(SectionTools.class);

    private final DocumentApiClient client;
    private final ObjectMapper objectMapper;

    public SectionTools(@RestClient final DocumentApiClient client, final ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "List all sections within a specific chapter. Returns section sequence, summary, and characters.")
    String listSectionsByChapter(
            @ToolArg(description = "The chapter ID to list sections for") final Long chapterId
    ) throws JsonProcessingException {
        LOG.infof("Tool call: listSectionsByChapter(chapterId=%d)", chapterId);
        final List<SectionResponse> sections = client.listSectionsByChapter(chapterId, false);
        return objectMapper.writeValueAsString(sections);
    }

    @Tool(description = "List sections for a document with pagination. Returns paged results across all chapters in the document.")
    String listSectionsByDocument(
            @ToolArg(description = "The document ID") final Long documentId,
            @ToolArg(description = "Page number (0-based)") final int page,
            @ToolArg(description = "Number of sections per page") final int pageSize
    ) throws JsonProcessingException {
        LOG.infof("Tool call: listSectionsByDocument(documentId=%d, page=%d, pageSize=%d)", documentId, page, pageSize);
        final PagedResponse<SectionResponse> response = client.listSectionsByDocument(documentId, false, page, pageSize);
        return objectMapper.writeValueAsString(response);
    }

    @Tool(description = "Get a section by its sequence number within a chapter.")
    String getSectionBySequence(
            @ToolArg(description = "The chapter ID") final Long chapterId,
            @ToolArg(description = "The sequence number of the section within the chapter") final Integer sequenceNumber
    ) throws JsonProcessingException {
        LOG.infof("Tool call: getSectionBySequence(chapterId=%d, seq=%d)", chapterId, sequenceNumber);
        final SectionResponse section = client.getSectionBySequence(chapterId, sequenceNumber, false);
        return objectMapper.writeValueAsString(section);
    }

    @Tool(description = "Get a specific section by its ID. Returns summary, characters, and optionally full text.")
    String getSection(
            @ToolArg(description = "The section ID") final Long id,
            @ToolArg(description = "Whether to include the full text of the section") final boolean includeFullText
    ) throws JsonProcessingException {
        LOG.infof("Tool call: getSection(id=%d, includeFullText=%b)", id, includeFullText);
        final SectionResponse section = client.getSection(id, includeFullText);
        return objectMapper.writeValueAsString(section);
    }
}
