package com.chriswininger.api.services.inferenceapi;

import com.chriswininger.ollama.OllamaApiService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OllamaApiServiceProducer {

    @Produces
    @ApplicationScoped
    public OllamaApiService ollamaApiService(
            @ConfigProperty(name = "ollama.base-url") final String baseUrl,
            @ConfigProperty(name = "ollama.model-name", defaultValue = "gemma4:e2b") final String modelName,
            @ConfigProperty(name = "ollama.num-ctx", defaultValue = "65536") final long numCtx,
            @ConfigProperty(name = "ollama.timeout-seconds", defaultValue = "300") final int timeoutSeconds,
            @ConfigProperty(name = "com.chriswininger.model.request-logging", defaultValue = "false")
            final boolean verboseRequestLogging
    ) {
        return new OllamaApiService(baseUrl, modelName, numCtx, timeoutSeconds, verboseRequestLogging);
    }
}
