package com.chriswininger.api.documents.services;

import com.chriswininger.ai.TocIsolationAiServiceFactory;
import com.chriswininger.api.dto.inferenceresults.TableOfContentsIsolationResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TableOfContentsIsolationService {

    private static final Logger LOG = Logger.getLogger(TableOfContentsIsolationService.class);

    private static final int TOC_PRESENCE_SAMPLE_SIZE = 3000;
    private static final int TOC_DETECTION_SAMPLE_SIZE = 16000;

    private final TocIsolationAiServiceFactory aiServiceFactory;

    public TableOfContentsIsolationService(final TocIsolationAiServiceFactory aiServiceFactory) {
        this.aiServiceFactory = aiServiceFactory;
    }

    public TableOfContentsIsolationResult isolateTableOfContents(final String document) {
        final String detectionSample = document.substring(0,
                Math.min(document.length(), TOC_PRESENCE_SAMPLE_SIZE));

        LOG.info("(isolateTableOfContents) starting Table of Contents presence detection");

        final var presenceResult = aiServiceFactory.createPresenceService().detect(detectionSample);

        LOG.infof("(isolateTableOfContents) presence detection finished. hasToc=%b", presenceResult.hasToc());

        if (!presenceResult.hasToc()) {
            LOG.info("(isolateTableOfContents) no Table of Contents detected, skipping boundary search");
            return new TableOfContentsIsolationResult(false, null, document);
        }

        final String frontOfBook = document.substring(0,
                Math.min(document.length(), TOC_DETECTION_SAMPLE_SIZE));

        final var tools = new TocIsolationTools(frontOfBook);
        final var aiService = aiServiceFactory.create(tools);

        LOG.info("(isolateTableOfContents) Table of Contents confirmed, starting boundary search");

        final String modelResponse = aiService.isolateToc(
                ("Locate the exact character boundaries of the Table of Contents in this document "
                + "and call markTocBoundary with the start and end indices. "
                + "Start by reading the beginning of the document with extractText, identify the "
                + "Table of Contents boundaries, then call markTocBoundary. "
                + "The document has %d characters.")
                        .formatted(frontOfBook.length()));

        LOG.infof("(isolateTableOfContents) boundary search finished. Response: %s", modelResponse);

        if (!tools.isBoundaryMarked()) {
            LOG.warn("(isolateTableOfContents) no boundary was marked despite Table of Contents being detected");
            return new TableOfContentsIsolationResult(false, null, document);
        }

        final int tocStart = Math.max(0, tools.getTocStart());
        final int tocEnd = Math.min(document.length(), tools.getTocEnd());

        if (tocStart >= tocEnd) {
            LOG.warnf("(isolateTableOfContents) invalid Table of Contents boundaries: [%d, %d)", tocStart, tocEnd);
            return new TableOfContentsIsolationResult(false, null, document);
        }

        final String tableOfContents = document.substring(tocStart, tocEnd);
        final String cleaned = document.substring(0, tocStart) + document.substring(tocEnd);

        LOG.infof("(isolateTableOfContents) removed Table of Contents [%d, %d). Before: %d chars, After: %d chars",
                tocStart, tocEnd, document.length(), cleaned.length());

        return new TableOfContentsIsolationResult(true, tableOfContents, cleaned);
    }
}
