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
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class ChapterSummaryAiServiceDirect {

    private static final String SUMMARIZE_SYSTEM_PROMPT = """
            You are a literary analysis assistant. You will be given a chapter from a book,
            including its heading and full text. Your task is to produce a plain-text analysis
            with three clearly labeled sections:

            SUMMARY:
            A concise paragraph summarizing the key events, ideas, or arguments of this chapter.
            Focus on what happens and why it matters to the narrative.

            CHARACTERS:
            A list of every character (person, creature, or named entity) who appears or is
            meaningfully mentioned in this chapter. List each character only once, do not repeat
            yourself. For example: Mr Dillon, James Dillon, James and Dillon should be listed
            only as James Dillon if they all refer to the same character. List one character
            per line.

            POSSIBLE QUESTIONS THIS ANSWERS:
            A list of questions a curious reader might have that this chapter directly answers.
            Phrase them as natural questions, e.g. "Who is Captain Aubrey?" or "What happens at
            the prize-taking?". List one question per line.

            Respond in plain text only. Do NOT use JSON, markdown, or code fences.""";

    private static final String STRUCTURE_SYSTEM_PROMPT = """
            You are a data formatting assistant. You will be given a plain-text literary analysis
            that contains a SUMMARY, a list of CHARACTERS, and a list of POSSIBLE QUESTIONS.

            Your job is to convert this text into a JSON object with exactly these three keys:
              "summary": string,
              "characters": array of strings,
              "possibleQuestionsThisAnswers": array of strings

            Rules:
            - Do not add, remove, or rephrase any content. Faithfully convert what is given.
            - Each character should appear exactly once in the array.
            - Respond with ONLY the JSON object. No markdown, no explanation, no code fences.""";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ConfigProperty(name = "ollama.base-url")
    String baseUrl;

    @ConfigProperty(name = "ollama.model-name")
    String modelName;

    @ConfigProperty(name = "ollama.timeout-seconds", defaultValue = "300")
    int timeoutSeconds;

    public ChapterSummary summarize(final String label, final String content) {
        try {
            // Pass 1: summarize into plain text (avoids structured-output repetition loops)
            final String plainTextSummary = summarizePlainText(label, content);

            // Pass 2: convert the plain-text analysis into structured JSON
            final String structurePayload = buildStructurePayload(plainTextSummary);
            final String structureResponse = callOllama(structurePayload);
            return parseResponse(structureResponse);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to call Ollama API", e);
        }
    }

    private String summarizePlainText(final String label, final String content) throws IOException, InterruptedException {
        final String payload = buildSummarizePayload(label, content);
        final String responseBody = callOllama(payload);

        final JsonNode outer = objectMapper.readTree(responseBody);
        return outer.path("message").path("content").asText();
    }

    private String callOllama(final String payload) throws IOException, InterruptedException {
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

        return response.body();
    }

    // Pass 1 payload: plain-text summarization (no structured format constraint)
    private String buildSummarizePayload(final String label, final String content) throws IOException {
        final String userMessage = "Chapter heading: " + label + "\n\nChapter text:\n" + content;

        final ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);
        root.put("stream", false);

        // options
        final ObjectNode options = root.putObject("options");
        options.put("num_ctx", 65536);

        // === Messages ===
        final ArrayNode messages = root.putArray("messages");
        final ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", SUMMARIZE_SYSTEM_PROMPT);
        final ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        // === End Messages ===

        return objectMapper.writeValueAsString(root);
    }

    // Pass 2 payload: convert plain-text summary into structured JSON
    private String buildStructurePayload(final String plainTextSummary) throws IOException {
        final ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);
        root.put("stream", false);

        // === format ===
        final ObjectNode format = root.putObject("format");
        format.put("type", "object");
        // format -> properties
        final ObjectNode properties = format.putObject("properties");
        // format -> properties -> summary
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
        final ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", STRUCTURE_SYSTEM_PROMPT);
        final ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", plainTextSummary);
        // === End Messages ===

        root.putArray("tools");

        return objectMapper.writeValueAsString(root);
    }

    private ChapterSummary parseResponse(final String responseBody) throws IOException {
        final JsonNode outer = objectMapper.readTree(responseBody);
        final String innerJson = outer.path("message").path("content").asText();
        final ChapterSummary chapterSummary = objectMapper.readValue(innerJson, ChapterSummary.class);

        // deduplicate characters
        final Set<String> deduplicatedCharacters = new HashSet<>(
                chapterSummary.characters().stream().map(String::toLowerCase).toList());
        return chapterSummary.withCharacters(deduplicatedCharacters.stream().toList());
    }
}
