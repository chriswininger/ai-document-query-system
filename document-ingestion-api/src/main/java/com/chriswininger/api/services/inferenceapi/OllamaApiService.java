package com.chriswininger.api.services.inferenceapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class OllamaApiService {
    @ConfigProperty(name = "ollama.base-url")
    String baseUrl;

    @ConfigProperty(name = "ollama.model-name", defaultValue = "gemma4:e2b")
    String modelName;

    @ConfigProperty(name = "ollama.timeout-seconds", defaultValue = "300")
    int timeoutSeconds;

    // TODO: Make ConfigProperty
    private final long numCtx = 65536L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger LOG = Logger.getLogger(OllamaApiService.class);

    public String callOllamaPlainTextResponse(
            final String systemPrompt,
            final String userMessage,
            final boolean think
    ) throws IOException, InterruptedException {
        final String payload = buildPayload(systemPrompt, userMessage, think);

        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        final var body = response.body();

        final JsonNode outer = objectMapper.readTree(body);

        if (think) {
            final String thinking = outer.path("message").path("thinking").asText();
            LOG.debugf("""
                    ===== Thinking ====
                    %s
                    ===================
                    """, thinking);
        }

        return outer.path("message").path("content").asText();
    }

    private String buildPayload(
            final String systemPrompt,
            final String userMessage,
            final boolean think
    ) throws IOException {
        final ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);
        root.put("stream", false);

        // options
        final ObjectNode options = root.putObject("options");
        options.put("num_ctx", 65536);

        if (think) {
            options.put("think", true);
        }

        // === Messages ===
        final ArrayNode messages = root.putArray("messages");
        final ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        final ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        // === End Messages ===

        return objectMapper.writeValueAsString(root);
    }

}
