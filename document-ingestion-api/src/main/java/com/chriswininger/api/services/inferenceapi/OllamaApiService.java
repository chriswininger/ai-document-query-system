package com.chriswininger.api.services.inferenceapi;

import com.chriswininger.api.dto.inferenceresults.InferenceDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class OllamaApiService {
    @ConfigProperty(name = "ollama.base-url")
    String baseUrl;

    @ConfigProperty(name = "ollama.model-name", defaultValue = "gemma4:e2b")
    String modelName;

    @ConfigProperty(name = "ollama.timeout-seconds", defaultValue = "300")
    int timeoutSeconds;

    @ConfigProperty(name = "com.chriswininger.model.request-logging", defaultValue = "false")
    Boolean verboseRequestLogging;

    // TODO: Make ConfigProperty
    private final long numCtx = 65536L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger LOG = Logger.getLogger(OllamaApiService.class);

    public String callOllamaPlainTextResponse(
            final String systemPrompt,
            final String userMessage,
            final boolean think
    ) throws IOException, InterruptedException {
        final String payload = buildPayload(systemPrompt, userMessage, think, null);

        if (verboseRequestLogging) {
            LOG.infof("OLLAMA request payload\n\n==========\n%s\n===========", payload);
        }

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

    public <T extends Record> T callOllamaStructuredResponse(
            final String systemPrompt,
            final String userMessage,
            final boolean think,
            final Class<T> recordClass
    ) throws IOException, InterruptedException {
        final ObjectNode format = buildFormatBlock(recordClass);
        final String payload = buildPayload(systemPrompt, userMessage, think, format);

        if (verboseRequestLogging) {
            LOG.infof("OLLAMA request payload\n\n==========\n%s\n===========", payload);
        }

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


        final String jsonResp = outer.path("message").path("content").asText();
        return parseToOutput(recordClass, jsonResp);
    }

    public <T extends Record> T parseToOutput(final Class<T> recordClass, final String json) throws JsonProcessingException {
        return objectMapper.readerFor(recordClass).readValue(json);
    }

    public String buildExampleJson(Class<? extends Record> recordClass) throws JsonProcessingException {
        final ObjectNode format = objectMapper.createObjectNode();

        final ObjectNode properties = format.putObject("properties");
        properties.put("type", "object");

        for (RecordComponent component : recordClass.getRecordComponents()) {
            final String name = component.getName();

            final String valueType = getType(component.getType(), component.getGenericType());
            properties.put(name, valueType);
        }

        return objectMapper.writeValueAsString(properties);
    }

    public ObjectNode buildFormatBlock(Class<? extends Record> recordClass) {
        final ObjectNode format = objectMapper.createObjectNode();
        format.put("type", "object");
        final ObjectNode properties = format.putObject("properties");
        final ArrayNode required = format.putArray("required");

        for (RecordComponent component : recordClass.getRecordComponents()) {
            final String name = component.getName();
            final ObjectNode prop = properties.putObject(name);

            mapType(prop, component.getType(), component.getGenericType());

            final InferenceDescription desc = component.getAnnotation(InferenceDescription.class);
            if (desc != null && desc.value().length > 0) {
                prop.put("description", String.join(" ", desc.value()));
            }

            required.add(name);
        }

        return format;
    }

    private String getType(Class<?> type, java.lang.reflect.Type genericType) {
        if (type == String.class) {
            return "string";
        } else if (type == Long.class || type == long.class
                || type == Integer.class || type == int.class) {
            return "integer";
        } else if (type == Double.class || type == double.class
                || type == Float.class || type == float.class) {
            return "number";
        } else if (type == Boolean.class || type == boolean.class) {
            return "boolean";
        } else if (type == List.class || type == Set.class) {
            /* final ObjectNode items = prop.putObject("items"); */
            if (genericType instanceof ParameterizedType pt) {
                final Class<?> elementType = (Class<?>) pt.getActualTypeArguments()[0];
                final String arrayType = getType(elementType, elementType);

                return arrayType + " []";
            } else {
                return "[]";
            }
        } else if (type.isRecord()) {
            @SuppressWarnings("unchecked")
            final Class<? extends Record> nested = (Class<? extends Record>) type;
            return "{}";
        } else {
            return "string";
        }
    }

    private void mapType(ObjectNode prop, Class<?> type, java.lang.reflect.Type genericType) {
        if (type == String.class) {
            prop.put("type", "string");
        } else if (type == Long.class || type == long.class
                || type == Integer.class || type == int.class) {
            prop.put("type", "integer");
        } else if (type == Double.class || type == double.class
                || type == Float.class || type == float.class) {
            prop.put("type", "number");
        } else if (type == Boolean.class || type == boolean.class) {
            prop.put("type", "boolean");
        } else if (type == List.class || type == Set.class) {
            prop.put("type", "array");
            final ObjectNode items = prop.putObject("items");
            if (genericType instanceof ParameterizedType pt) {
                final Class<?> elementType = (Class<?>) pt.getActualTypeArguments()[0];
                mapType(items, elementType, elementType);
            }
        } else if (type.isRecord()) {
            @SuppressWarnings("unchecked")
            final Class<? extends Record> nested = (Class<? extends Record>) type;
            prop.setAll(buildFormatBlock(nested));
        } else {
            prop.put("type", "string");
        }
    }

    private String buildPayload(
            final String systemPrompt,
            final String userMessage,
            final boolean think,
            final ObjectNode format
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

        if (Objects.nonNull(format)) {
            root.set("format", format);
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
