package com.chriswininger.api.documents.services;

import com.chriswininger.ai.TocIsolationAiServiceFactory;
import com.chriswininger.api.dto.inferenceresults.TableOfContentsIsolationResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TableOfContentsIsolationService {

    private static final Logger LOG = Logger.getLogger(TableOfContentsIsolationService.class);

    private static final int TOC_DETECTION_SAMPLE_SIZE = 16000;

    private final TocIsolationAiServiceFactory aiServiceFactory;

    public TableOfContentsIsolationService(final TocIsolationAiServiceFactory aiServiceFactory) {
        this.aiServiceFactory = aiServiceFactory;
    }

    public TableOfContentsIsolationResult isolateTableOfContents(final String document) {
        final String frontOfBook = document.substring(0,
                Math.min(document.length(), TOC_DETECTION_SAMPLE_SIZE));

        final var tools = new TocIsolationTools(frontOfBook);
        final var aiService = aiServiceFactory.create(tools);

        LOG.info("(isolateTableOfContents) starting tool-based TOC detection");

        final String modelResponse = aiService.isolateToc(
                "Analyze this document to determine if it contains a Table of Contents. "
                + "Start by reading the beginning of the document with extractText, then search "
                + "for TOC indicators. If you find a TOC, identify its exact boundaries and call "
                + "markTocBoundary. The document has %d characters.".formatted(frontOfBook.length()));

        LOG.infof("(isolateTableOfContents) model finished. Response: %s", modelResponse);

        if (!tools.isBoundaryMarked()) {
            LOG.info("(isolateTableOfContents) no TOC boundary was marked");
            return new TableOfContentsIsolationResult(false, null, document);
        }

        final int tocStart = Math.max(0, tools.getTocStart());
        final int tocEnd = Math.min(document.length(), tools.getTocEnd());

        if (tocStart >= tocEnd) {
            LOG.warnf("(isolateTableOfContents) invalid TOC boundaries: [%d, %d)", tocStart, tocEnd);
            return new TableOfContentsIsolationResult(false, null, document);
        }

        final String tableOfContents = document.substring(tocStart, tocEnd);
        final String cleaned = document.substring(0, tocStart) + document.substring(tocEnd);

        LOG.infof("(isolateTableOfContents) removed TOC [%d, %d). Before: %d chars, After: %d chars",
                tocStart, tocEnd, document.length(), cleaned.length());

        return new TableOfContentsIsolationResult(true, tableOfContents, cleaned);
    }
}
