package com.chriswininger.ollama;

public record OllamaRequestOptions(
    Boolean think,
    String model,
    Long numCtx,
    String apiKey,
    String baseUrl
) {
    public static OllamaRequestOptions defaults() {
        return new OllamaRequestOptions(null, null, null, null, null);
    }

    public static OllamaRequestOptions withThink(boolean think) {
        return new OllamaRequestOptions(think, null, null, null, null);
    }
}
