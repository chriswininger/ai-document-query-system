package com.chriswininger.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class OllamaModelProducer {

    @ConfigProperty(name = "ollama.base-url")
    String baseUrl;

    @ConfigProperty(name = "ollama.model-name")
    String modelName;

    @ConfigProperty(name = "ollama.embedding-model-name")
    String embeddingModelName;

    @ConfigProperty(name = "ollama.timeout-seconds", defaultValue = "120")
    long timeoutSeconds;

    // TODO: We are not using this anymore
    @Produces
    @ApplicationScoped
    public ChatModel chatModel() {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Produces
    @ApplicationScoped
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
