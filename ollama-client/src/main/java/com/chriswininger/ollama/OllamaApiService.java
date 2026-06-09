package com.chriswininger.ollama;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

public class OllamaApiService {

    private static final Logger LOG = Logger.getLogger(OllamaApiService.class);

    private final String baseUrl;
    private final String modelName;
    private final long numCtx;
    private final int timeoutSeconds;
    private final boolean verboseRequestLogging;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaApiService(
            final String baseUrl,
            final String modelName,
            final long numCtx,
            final int timeoutSeconds,
            final boolean verboseRequestLogging
    ) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.numCtx = numCtx;
        this.timeoutSeconds = timeoutSeconds;
        this.verboseRequestLogging = verboseRequestLogging;
    }

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

        LOG.info("!!! body: " + outer);
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
        return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(buildExampleNode(recordClass));
    }

    private ObjectNode buildExampleNode(Class<? extends Record> recordClass) {
        final ObjectNode node = objectMapper.createObjectNode();

        for (RecordComponent component : recordClass.getRecordComponents()) {
            final String name = component.getName();
            final Class<?> type = component.getType();
            final java.lang.reflect.Type genericType = component.getGenericType();

            setExampleValue(node, name, type, genericType);
        }

        return node;
    }

    private void setExampleValue(ObjectNode parent, String fieldName, Class<?> type, java.lang.reflect.Type genericType) {
        if (type == String.class) {
            parent.put(fieldName, "string");
        } else if (type == Long.class || type == long.class) {
            parent.put(fieldName, 0L);
        } else if (type == Integer.class || type == int.class) {
            parent.put(fieldName, 0);
        } else if (type == Double.class || type == double.class
                || type == Float.class || type == float.class) {
            parent.put(fieldName, 0.0);
        } else if (type == Boolean.class || type == boolean.class) {
            parent.put(fieldName, false);
        } else if (type == List.class || type == Set.class) {
            final ArrayNode arr = parent.putArray(fieldName);
            if (genericType instanceof ParameterizedType pt) {
                final Class<?> elementType = (Class<?>) pt.getActualTypeArguments()[0];
                if (elementType.isRecord()) {
                    @SuppressWarnings("unchecked")
                    final Class<? extends Record> nested = (Class<? extends Record>) elementType;
                    arr.add(buildExampleNode(nested));
                } else {
                    arr.add(getExamplePrimitive(elementType));
                }
            }
        } else if (type.isRecord()) {
            @SuppressWarnings("unchecked")
            final Class<? extends Record> nested = (Class<? extends Record>) type;
            parent.set(fieldName, buildExampleNode(nested));
        } else {
            parent.put(fieldName, "string");
        }
    }

    private String getExamplePrimitive(Class<?> type) {
        if (type == Long.class || type == long.class
                || type == Integer.class || type == int.class) {
            return "0";
        } else if (type == Boolean.class || type == boolean.class) {
            return "false";
        }
        return "string";
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

        final ObjectNode options = root.putObject("options");
        options.put("num_ctx", numCtx);

        if (think) {
            options.put("think", true);
        }

        if (Objects.nonNull(format)) {
            root.set("format", format);
        }

        final ArrayNode messages = root.putArray("messages");
        final ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        final ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        return objectMapper.writeValueAsString(root);
    }
}
