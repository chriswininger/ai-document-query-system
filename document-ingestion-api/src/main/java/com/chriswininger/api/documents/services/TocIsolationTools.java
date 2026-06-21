package com.chriswininger.api.documents.services;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.jboss.logging.Logger;

public class TocIsolationTools {

    private static final Logger LOG = Logger.getLogger(TocIsolationTools.class);

    private static final int MAX_EXTRACT_LENGTH = 2000;

    private final String document;

    private int tocStart = -1;
    private int tocEnd = -1;
    private boolean boundaryMarked = false;

    public TocIsolationTools(final String document) {
        this.document = document;
    }

    @Tool("Returns the total number of characters in the document.")
    public int documentLength() {
        LOG.debugf("(documentLength) returning %d", document.length());
        return document.length();
    }

    @Tool("Extracts text from the document between startIndex (inclusive) and endIndex (exclusive). "
            + "The window is capped at 2000 characters. Use this to read portions of the document.")
    public String extractText(
            @P("The inclusive start index") final int startIndex,
            @P("The exclusive end index") final int endIndex
    ) {
        final int clampedStart = Math.max(0, startIndex);
        final int clampedEnd = Math.min(document.length(), endIndex);
        final int effectiveEnd = Math.min(clampedEnd, clampedStart + MAX_EXTRACT_LENGTH);

        LOG.debugf("(extractText) [%d, %d) -> clamped [%d, %d)", startIndex, endIndex, clampedStart, effectiveEnd);
        return document.substring(clampedStart, effectiveEnd);
    }

    @Tool("Searches for the first occurrence of a query string in the document starting at fromIndex. "
            + "Returns the character index where the match begins, or -1 if not found.")
    public int searchText(
            @P("The text to search for") final String query,
            @P("The index to start searching from") final int fromIndex
    ) {
        final int clampedFrom = Math.max(0, fromIndex);
        final int result = document.indexOf(query, clampedFrom);
        LOG.debugf("(searchText) query='%s' fromIndex=%d -> %d", query, clampedFrom, result);
        return result;
    }

    @Tool("Marks the start and end character indices of the Table of Contents. "
            + "Call this once you have identified the exact TOC boundaries. "
            + "startIndex is inclusive, endIndex is exclusive.")
    public String markTocBoundary(
            @P("The inclusive start index of the TOC") final int startIndex,
            @P("The exclusive end index of the TOC") final int endIndex
    ) {
        this.tocStart = startIndex;
        this.tocEnd = endIndex;
        this.boundaryMarked = true;
        LOG.infof("(markTocBoundary) TOC boundary marked: [%d, %d)", startIndex, endIndex);
        return "TOC boundary recorded: [%d, %d)".formatted(startIndex, endIndex);
    }

    public boolean isBoundaryMarked() {
        return boundaryMarked;
    }

    public int getTocStart() {
        return tocStart;
    }

    public int getTocEnd() {
        return tocEnd;
    }
}
