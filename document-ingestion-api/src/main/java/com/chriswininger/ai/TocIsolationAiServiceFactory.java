package com.chriswininger.ai;

import com.chriswininger.api.documents.services.TocIsolationAiService;
import com.chriswininger.api.documents.services.TocIsolationTools;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class TocIsolationAiServiceFactory {

    @ConfigProperty(name = "ollama.base-url")
    String baseUrl;

    @ConfigProperty(name = "ollama.toc-isolation.model-name", defaultValue = "gemma4:e4b")
    String modelName;

    @ConfigProperty(name = "ollama.toc-isolation.num-ctx", defaultValue = "32768")
    int numCtx;

    @ConfigProperty(name = "ollama.timeout-seconds", defaultValue = "120")
    long timeoutSeconds;

    public TocIsolationAiService create(final TocIsolationTools tools) {
        final var model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .numCtx(numCtx)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        return AiServices.builder(TocIsolationAiService.class)
                .chatModel(model)
                .tools(tools)
                .maxSequentialToolsInvocations(25)
                .build();
    }
}
