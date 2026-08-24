package com.chriswininger.api.documents.services;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.jboss.logging.Logger;

public class TocIsolationTools {

    private static final Logger LOG = Logger.getLogger(TocIsolationTools.class);

    private static final int MAX_EXTRACT_LENGTH = 6000;

    private final String document;

    private int tocStart = -1;
    private int tocEnd = -1;
    private boolean boundaryMarked = false;

    public TocIsolationTools(final String document) {
        this.document = document;
    }

    @Tool("Returns the total number of characters in the document.")
    public int documentLength() {
        LOG.infof("(documentLength) returning %d", document.length());
        return document.length();
    }

    @Tool("Extracts text from the document between startIndex (inclusive) and endIndex (exclusive). "
            + "The window is capped at 6000 characters. Use this to read portions of the document.")
    public String extractText(
            @P("The inclusive start index") final int startIndex,
            @P("The exclusive end index") final int endIndex
    ) {
        final int clampedStart = Math.max(0, startIndex);
        final int clampedEnd = Math.min(document.length(), endIndex);
        final int effectiveEnd = Math.min(clampedEnd, clampedStart + MAX_EXTRACT_LENGTH);

        LOG.infof("(extractText) [%d, %d) -> clamped [%d, %d)", startIndex, endIndex, clampedStart, effectiveEnd);
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
        LOG.infof("(searchText) query='%s' fromIndex=%d -> %d", query, clampedFrom, result);
        return result;
    }

    @Tool("Extracts up to lineCount lines starting from the line that contains fromIndex. "
            + "Returns the text prefixed with the character offset of the first returned line, "
            + "e.g. '[charOffset=2018]\\n...'. Total output is capped at 6000 characters. "
            + "Use this instead of extractText when you want to read content line by line "
            + "and need the exact starting character offset of the result.")
    public String extractLines(
            @P("Character index of any position within the first line to return") final int fromIndex,
            @P("Maximum number of lines to return") final int lineCount
    ) {
        final int clamped = Math.max(0, Math.min(document.length() - 1, fromIndex));
        final int firstLineStart = document.lastIndexOf('\n', clamped - 1) + 1;

        int pos = firstLineStart;
        int linesCollected = 0;
        final StringBuilder sb = new StringBuilder();

        while (linesCollected < lineCount && pos < document.length() && sb.length() < MAX_EXTRACT_LENGTH) {
            final int nlPos = document.indexOf('\n', pos);
            final int lineEnd = nlPos == -1 ? document.length() : nlPos + 1;
            final String line = document.substring(pos, Math.min(lineEnd, document.length()));
            if (sb.length() + line.length() > MAX_EXTRACT_LENGTH) {
                break;
            }
            sb.append(line);
            linesCollected++;
            pos = lineEnd;
            if (nlPos == -1) {
                break;
            }
        }

        LOG.infof("(extractLines) fromIndex=%d lineCount=%d -> charOffset=%d lines=%d chars=%d",
                fromIndex, lineCount, firstLineStart, linesCollected, sb.length());
        return "[charOffset=%d]\n%s".formatted(firstLineStart, sb);
    }

    @Tool("Returns the start (inclusive) and end (exclusive) character indices of the line "
            + "containing the given character index. Use this to snap a rough index to a clean "
            + "line boundary before calling markTocBoundary.")
    public int[] getLineRange(
            @P("Any character index within the line") final int characterIndex
    ) {
        final int clamped = Math.max(0, Math.min(document.length() - 1, characterIndex));
        final int lineStart = document.lastIndexOf('\n', clamped - 1) + 1;
        final int rawEnd = document.indexOf('\n', clamped);
        final int lineEnd = rawEnd == -1 ? document.length() : rawEnd + 1;
        LOG.infof("(getLineRange) index=%d -> line [%d, %d)", characterIndex, lineStart, lineEnd);
        return new int[]{lineStart, lineEnd};
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
