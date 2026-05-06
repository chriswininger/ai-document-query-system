package com.chriswininger.ai;

import com.chriswininger.api.services.ChapterSummaryAiService;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class ChapterSummaryAiServiceProducer {

    @ConfigProperty(name = "ollama.base-url")
    String baseUrl;

    @ConfigProperty(name = "ollama.model-name")
    String modelName;

    @ConfigProperty(name = "ollama.timeout-seconds", defaultValue = "120")
    long timeoutSeconds;

    @ConfigProperty(name = "ollama.num-ctx", defaultValue = "16384")
    int numCtx;

    @Produces
    @ApplicationScoped
    public ChapterSummaryAiService chapterSummaryAiService() {
        ChatModel jsonModel = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .numCtx(numCtx)
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .build();

        return AiServices.create(ChapterSummaryAiService.class, jsonModel);
    }
}
