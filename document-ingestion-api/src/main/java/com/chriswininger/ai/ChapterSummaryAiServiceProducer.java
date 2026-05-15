package com.chriswininger.ai;

import com.chriswininger.api.DocumentResource;
import com.chriswininger.api.services.ChapterSummaryAiService;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;

@ApplicationScoped
public class ChapterSummaryAiServiceProducer {
    private static final Logger LOG = Logger.getLogger(ChapterSummaryAiServiceProducer.class);

    @ConfigProperty(name = "ollama.base-url")
    String baseUrl;

    @ConfigProperty(name = "ollama.model-name")
    String modelName;

    @ConfigProperty(name = "ollama.timeout-seconds", defaultValue = "120")
    long timeoutSeconds;

    @ConfigProperty(name = "ollama.num-ctx", defaultValue = "131072")
    int numCtx;

    @ConfigProperty(name = "com.chriswininger.model.request-logging", defaultValue = "false")
    boolean requestLogging;

    @Produces
    @ApplicationScoped
    public ChapterSummaryAiService chapterSummaryAiService() {
        ChatModel jsonModel = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .numCtx(numCtx)
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .logRequests(requestLogging)
                .logResponses(requestLogging)
                .build();

        return AiServices.builder(ChapterSummaryAiService.class)
                .chatModel(jsonModel)
                .chatRequestTransformer(req -> {
                    LOG.info("num messages: " + req.messages().size());
                    if (req.messages().size() > 2) {
                        // hopefully this is solved now, but leaving this in place just in case
                        // https://github.com/quarkiverse/quarkus-langchain4j/issues/2071
                        LOG.warn("Warning stale messages may be getting sent to the model: " + req.messages().size());
                    }

                    return req;
                })
                .build();
    }
}
