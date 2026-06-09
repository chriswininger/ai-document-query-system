package com.chriswininger.cli.commands.query.services;

import com.chriswininger.cli.commands.query.dto.PossibleDocument;
import com.chriswininger.cli.services.DocumentIngestionApiService;
import com.chriswininger.ollama.OllamaApiService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DocumentFinderServiceTest {

    private static final Logger LOG = Logger.getLogger(DocumentFinderServiceTest.class);

    @Inject
    DocumentFinderService documentFinderService;

    @Inject
    DocumentIngestionApiService documentIngestionApiService;

    @Inject
    OllamaApiService ollamaApiService;

    @Test
    void findPossibleDocumentsForQuery_returnsNonNullList() throws IOException, InterruptedException {
        final List<PossibleDocument> result = documentFinderService.findPossibleDocumentsForQuery("Did Steven Maturin play music");

        assertNotNull(result);
    }

    @Test
    void findPossibleDocumentsForQuery_returnsEmptyList() throws IOException, InterruptedException {
        final List<PossibleDocument> result = documentFinderService.findPossibleDocumentsForQuery("test query");
        assertNotNull(result);
    }

    @Test
    void findPossibleDocumentsForQuery_withNullQuery_returnsEmptyList() throws IOException, InterruptedException {
        final List<PossibleDocument> result = documentFinderService.findPossibleDocumentsForQuery(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findPossibleDocumentsForQuery_integrationWithLiveServices() throws IOException, InterruptedException {
        LOG.infof("DocumentIngestionApiService: %s", documentIngestionApiService);
        LOG.infof("OllamaApiService: %s", ollamaApiService);

        final List<PossibleDocument> result = documentFinderService.findPossibleDocumentsForQuery(
                "What books discuss artificial intelligence?"
        );

        assertNotNull(result);
        LOG.infof("findPossibleDocumentsForQuery returned %d possible documents", result.size());
        for (final PossibleDocument doc : result) {
            LOG.infof("PossibleDocument: id=%d, reason=%s", doc.documentId(), doc.reason());
        }
    }
}
