package com.chriswininger.api.services;

import com.chriswininger.api.dto.ChapterSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class ChapterSummaryAiServiceDirect {

    private static final String SYSTEM_PROMPT = """
            You are a literary analysis assistant. You will be given a chapter from a book,
            including its heading and full text. Your task is to produce:

            1. summary: A concise paragraph summarizing the key events, ideas, or arguments
               of this chapter. Focus on what happens and why it matters to the narrative.

            2. characters: A list of every character (person, creature, or named entity) who
               appears or is meaningfully mentioned in this chapter. List each character only once,
               do not repeat yourself. For example: Mr Dillon, James Dillon, James and Dillon should
               be listed only as James Dillon if they all refer to the same character.

            3. possibleQuestionsThisAnswers: A list of questions a curious reader might have
               that this chapter directly answers. Phrase them as natural questions, e.g.
               "Who is Captain Aubrey?" or "What happens at the prize-taking?".

            You MUST respond with ONLY a valid JSON object. No markdown, no explanation, no code fences.
            The JSON object must have exactly these three keys:
              "summary": string,
              "characters": array of strings,
              "possibleQuestionsThisAnswers": array of strings""";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ConfigProperty(name = "ollama.base-url")
    String baseUrl;

    @ConfigProperty(name = "ollama.model-name")
    String modelName;

    @ConfigProperty(name = "ollama.timeout-seconds", defaultValue = "300")
    int timeoutSeconds;

    public ChapterSummary summarize(String label, String content) {
        try {
            String payload = buildPayload(label, content);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
            }

            return parseResponse(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("!!! Failed to call Ollama API", e);
        }
    }

    private String buildPayload(String label, String content) throws IOException {
        final String userMessage = "Chapter heading: " + label + "\n\nChapter text:\n" + content;

        final ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);
        root.put("stream", false);

        // === format ===
        final ObjectNode format = root.putObject("format");
        format.put("type", "object");
        // format -> properties
        final ObjectNode properties = format.putObject("properties");
        // format -> properties -> characters -> summary
        properties.putObject("summary").put("type", "string");
        // format -> properties -> characters
        final ObjectNode chars = properties.putObject("characters");
        chars.put("type", "array");
        chars.putObject("items").put("type", "string");
        // format -> properties -> possibleQuestionsThisAnswers
        final ObjectNode questions = properties.putObject("possibleQuestionsThisAnswers");
        questions.put("type", "array");
        questions.putObject("items").put("type", "string");
        // === end format ===

        // options
        final ObjectNode options = root.putObject("options");
        options.put("num_ctx", 65536);

        // === Messages ===
        final ArrayNode messages = root.putArray("messages");
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        final ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        // End Messages ===

        // !!! WAIT IT's A String this whole time lol
        return objectMapper.writeValueAsString(root);
    }

    private ChapterSummary parseResponse(String responseBody) throws IOException {
        JsonNode outer = objectMapper.readTree(responseBody);
        String innerJson = outer.path("message").path("content").asText();
        return objectMapper.readValue(innerJson, ChapterSummary.class);
    }
}
