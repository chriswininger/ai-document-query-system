package com.chriswininger.cli.services;

import com.chriswininger.client.ApiException;
import com.chriswininger.client.api.DocumentResourceApi;
import com.chriswininger.client.model.DocumentResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class DocumentIngestionApiService {

    private static final Logger LOG = Logger.getLogger(DocumentIngestionApiService.class);

    private final DocumentResourceApi documentResourceApi;

    public DocumentIngestionApiService(final DocumentResourceApi documentResourceApi) {
        this.documentResourceApi = documentResourceApi;
    }

    public List<DocumentResponse> getDocuments() {
        LOG.infof("GET /rest/v1/documents");

        try {
            final List<DocumentResponse> documents = documentResourceApi.listDocuments(false);
            LOG.infof("Retrieved %d documents from API", documents.size());
            return documents;
        } catch (ApiException e) {
            throw new RuntimeException(
                    "document-ingestion-api returned HTTP " + e.getCode() + ": " + e.getMessage(),
                    e
            );
        }
    }
}
