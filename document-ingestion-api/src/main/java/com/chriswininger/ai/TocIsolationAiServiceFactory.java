package com.chriswininger.ai;

import com.chriswininger.api.documents.services.TocIsolationAiService;
import com.chriswininger.api.documents.services.TocIsolationTools;
import com.chriswininger.api.documents.services.TocPresenceAiService;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class TocIsolationAiServiceFactory {

    @ConfigProperty(name = "ollama.toc-isolation.base-url", defaultValue = "https://ollama.com")
    String baseUrl;

    @ConfigProperty(name = "ollama.toc-isolation.model-name", defaultValue = "gemma4:e4b")
    String modelName;

    @ConfigProperty(name = "ollama.toc-isolation.num-ctx", defaultValue = "32768")
    int numCtx;

    @ConfigProperty(name = "ollama.timeout-seconds", defaultValue = "120")
    long timeoutSeconds;

    @ConfigProperty(name = "ollama.toc-isolation.api-key")
    Optional<String> apiKey;

    public TocPresenceAiService createPresenceService() {
        final var builder = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .numCtx(numCtx)
                .think(true)
                .timeout(Duration.ofSeconds(timeoutSeconds));

        apiKey.filter(key -> !key.isBlank())
                .ifPresent(key -> builder.customHeaders(Map.of("Authorization", "Bearer " + key)));

        final var model = builder.build();

        return AiServices.create(TocPresenceAiService.class, model);
    }

    public TocIsolationAiService create(final TocIsolationTools tools) {
        final var builder = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .numCtx(numCtx)
                .think(true)
                .timeout(Duration.ofSeconds(timeoutSeconds));

        apiKey.filter(key -> !key.isBlank())
                .ifPresent(key -> builder.customHeaders(Map.of("Authorization", "Bearer " + key)));

        final var model = builder.build();

        return AiServices.builder(TocIsolationAiService.class)
                .chatModel(model)
                .tools(tools)
                .maxSequentialToolsInvocations(125)
                .build();
    }
}
