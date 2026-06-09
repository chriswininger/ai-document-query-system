package com.chriswininger.cli.services;

import com.chriswininger.client.ApiClient;
import com.chriswininger.client.api.DocumentResourceApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class DocumentIngestionApiClientProducer {

    @Produces
    @ApplicationScoped
    public DocumentResourceApi documentResourceApi(
            @ConfigProperty(name = "document-ingestion-api.base-url", defaultValue = "http://localhost:8080")
            final String baseUrl,
            @ConfigProperty(name = "document-ingestion-api.timeout-seconds", defaultValue = "30")
            final int timeoutSeconds
    ) {
        final ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(baseUrl);
        apiClient.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        apiClient.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return new DocumentResourceApi(apiClient);
    }
}
