package com.wininger.spring_ai_demo.conversations;

import com.wininger.spring_ai_demo.api.chat.ChatRequest;
import com.wininger.spring_ai_demo.api.chat.ChatResponse;
import com.wininger.spring_ai_demo.api.chat.ChatStreamingResponseItem;
import com.wininger.spring_ai_demo.api.chat.ChatStreamingResponseItemType;
import com.wininger.spring_ai_demo.api.rag.VectorSearchResult;
import com.wininger.spring_ai_demo.api.rag.VectorSearchService;
import com.wininger.spring_ai_demo.logging.LoggingAdvisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.wininger.spring_ai_demo.api.chat.ChatStreamingResponseItemType.CONTENT;
import static com.wininger.spring_ai_demo.api.chat.ChatStreamingResponseItemType.RAG_DOCUMENT;
import static com.wininger.spring_ai_demo.api.chat.ChatStreamingResponseItemType.THINKING;
import static java.util.Objects.nonNull;

@Service
public class ConversationService {
    // TODO: I can get this from the chatResponse
    @Value("${spring.ai.ollama.chat.options.model}")
    private String llmModel;

    @Value("${spring.ai.ollama.chat.options.num-ctx}")
    private int contextWindow;

    private final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final AtomicInteger lastId = new AtomicInteger(0);

    private final ChatClient chatClient;

    private final VectorStore vectorStore;

    private final VectorSearchService vectorSearchService;

    private final TokenCountEstimator tokenCountEstimator;

    private final OllamaChatModel ollamaChatModel;

    public ConversationService(
        final ChatClient.Builder chatClientBuilder,
        final VectorStore vectorStore,
        final VectorSearchService vectorSearchService,
        final TokenCountEstimator tokenCountEstimator,
        final OllamaChatModel ollamaChatModel
    ) {
        this.ollamaChatModel = ollamaChatModel;

        // ==================================
        InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .build();

        // 2. Pass the repository to the static builder() method as a required argument
        MessageChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
            .build();

        this.chatClient = chatClientBuilder
            .defaultAdvisors(
                chatMemoryAdvisor,
                new LoggingAdvisor()
            )
            .defaultSystem("""
                You are a helpful assistant. You are confident in your answers. Your answers are short and to the point.
                If you do not know something you simply say so. Please do not explain your thinking, just answer the
                question.
                """)
            .build();

        this.vectorSearchService = vectorSearchService;
        this.tokenCountEstimator = tokenCountEstimator;
        this.vectorStore = vectorStore;
    }

    public final Integer getNextConversationId() {
        return lastId.addAndGet(1);
    }

    public ChatResponse performConversationExchange(
        final ChatRequest chatRequest
    ) {
        final var startTime = new Date();
        final int conversationId = nonNull(chatRequest.conversationId())
            ? chatRequest.conversationId()
            : getNextConversationId();

        log.info("processing /api/v1/chat at {}", startTime);

        final int topK = chatRequest.numberOfRagDocumentsToInclude() != null
            ? chatRequest.numberOfRagDocumentsToInclude()
            : 5;

        final AtomicReference<List<Document>> ragDocsRef = new AtomicReference<>();

        final ChatClientRequestSpec prompt = chatClient
            .prompt()
            .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId));


            if (topK > 0 && !chatRequest.documentSourceIds().isEmpty()) {
                prompt.advisors(questionAnswerAdvisor(vectorStore, topK, chatRequest.documentSourceIds()));
            }

            prompt.advisors(new RagDocumentCaptureAdvisor(ragDocsRef));

        if (nonNull(chatRequest.systemPrompt())) {
          prompt.system(chatRequest.systemPrompt());
        }

        final var userPromptInfo = getUserPromptWithRagAugmentation(chatRequest);
        final var promptWithRag = userPromptInfo.userPrompt();
        final var estimatedTokens = tokenCountEstimator.estimate(promptWithRag);

        log.info("Constructed prompt is '{}' characters", promptWithRag.length());
        log.info("Constructed prompt is around '{}' tokens", estimatedTokens);

        if (estimatedTokens >= contextWindow) {
            log.warn("The number of tokens will exceed the context window of '{}'", contextWindow);
        }

        prompt.user(promptWithRag);

        final var modelResponse = prompt
            .call();

       final var chatResponse = modelResponse.chatResponse();

        final String thinking;
        final String output;
       if (nonNull(chatResponse)) {
           final var promptTokens = chatResponse.getMetadata().getUsage().getPromptTokens();
           final var completionTokens = chatResponse.getMetadata().getUsage().getCompletionTokens();
           final var totalTokens = chatResponse.getMetadata().getUsage().getTotalTokens();

           log.info("promptTokens: {}, completionTokens: {}, totalTokens: {}", promptTokens, completionTokens, totalTokens);

           thinking = chatResponse.getResult().getMetadata().get("thinking");
           output = chatResponse.getResult().getOutput().getText();

           log.debug("======================");
           log.debug("thinking: {}\n\n", thinking);
           log.debug("output: {}", output);
           log.debug("======================");
       } else {
           thinking = "";
           output = "";
       }

        final List<Document> docs = nonNull(ragDocsRef.get()) ? ragDocsRef.get() : List.of();
        log.info("found {} RAG documents", docs.size());
        return new ChatResponse(
            chatRequest.userPrompt(),
            output,
            thinking,
            docs.stream().map(doc ->
                new VectorSearchResult(doc.getText(), doc.getMetadata(), doc.getScore())
            ).toList(),
            llmModel,
            conversationId,
            startTime,
            new Date()
        );
    }

    // TODO See If we can track Rag Docs
    public Flux<ChatStreamingResponseItem> performConversationExchangeStreaming(
        final ChatRequest chatRequest
    ) {
        final var startTime = new Date();
        final int conversationId = nonNull(chatRequest.conversationId())
            ? chatRequest.conversationId()
            : getNextConversationId();

        log.info("processing /api/v1/chat/generic/stream at {}", startTime);

        final int topK = chatRequest.numberOfRagDocumentsToInclude() != null
            ? chatRequest.numberOfRagDocumentsToInclude()
            : 5;

        // Track the list of rag documents returned by the pipeline so that we can stream them to the client
        final AtomicReference<List<Document>> ragDocsRef = new AtomicReference<>();
        final ChatClientRequestSpec prompt = chatClient
            .prompt()
            .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId))
            .advisors(questionAnswerAdvisor(vectorStore, topK, chatRequest.documentSourceIds()))
            .advisors(new RagDocumentCaptureAdvisor(ragDocsRef)); // TODO: I think I can actually get these from modelResponse object

        if (nonNull(chatRequest.systemPrompt())) {
            prompt.system(chatRequest.systemPrompt());
        }

        final var userPromptInfo = getUserPromptWithRagAugmentation(chatRequest);
        final var promptWithRag = userPromptInfo.userPrompt();
        final var estimatedTokens = tokenCountEstimator.estimate(promptWithRag);

        log.info("Constructed prompt is '{}' characters", promptWithRag.length());
        log.info("Constructed prompt is around '{}' tokens", estimatedTokens);

        if (estimatedTokens >= contextWindow) {
            log.warn("The number of tokens will exceed the context window of '{}'", contextWindow);
        }

        prompt.user(promptWithRag);

        return prompt
            .stream()
            .chatResponse()
            .map(chatResponse -> {
                // will always be null due to this issue https://github.com/spring-projects/spring-ai/issues/4866
                // there is a fix, watch for it to make it into a release
                final String thinking = chatResponse.getResult().getMetadata().get("thinking");
                final String output = chatResponse.getResult().getOutput().getText();

                /*
                final ChatStreamingResponseItemType type = (nonNull(thinking) && !thinking.isEmpty())
                    ? THINKING : CONTENT;*/

                // switch to the above version when thinking bug is fixed https://github.com/spring-projects/spring-ai/issues/4866
                final ChatStreamingResponseItemType itemType = (nonNull(output) && !output.isEmpty())
                    ? CONTENT : THINKING;

                final String text = nonNull(thinking) && !thinking.isEmpty() ? thinking : output;

                return new ChatStreamingResponseItem(llmModel, conversationId, itemType, text);
            })
            .concatWith(Flux.defer(() -> {
                log.debug("Concat documents to stream");
                final List<Document> docs = nonNull(ragDocsRef.get()) ? ragDocsRef.get() : List.of();
                return Flux.fromIterable(docs)
                    .map(doc -> new ChatStreamingResponseItem(
                        llmModel,
                        conversationId,
                        RAG_DOCUMENT,
                        doc.getText()
                    ));
            }));
    }

    public ChatResponse performTestChat(final String userPrompt) {
        var response = this.ollamaChatModel.call(
            new Prompt(
                userPrompt,
                OllamaChatOptions.builder()
                    .enableThinking()
                    .build()
            ));

        final String thinking = response.getResult().getMetadata().get("thinking");
        String answer = response.getResult().getOutput().getText();

        return new ChatResponse(
            userPrompt,
            answer,
            thinking,
            new ArrayList<>(),
            llmModel,
            0,
            new Date(),
            new Date()
        );
    }

    public Flux<ChatResponse> performTestChatStream(final String userPrompt) {
        return this.ollamaChatModel.stream(
            new Prompt(
                userPrompt,
                OllamaChatOptions.builder()
                    .enableThinking()
                    .build()
            )).map((chatResponse) -> {


                // will always be null due to this issue https://github.com/spring-projects/spring-ai/issues/4866
                // there is a fix, watch for it to make it into a release
                final String thinking = chatResponse.getResult().getMetadata().get("thinking");
                if (thinking != null && !thinking.isEmpty()) {
                    System.out.println("!!! THINKING");
                    System.out.println("[Thinking] " + thinking);
                }

                String answer = chatResponse.getResult().getOutput().getText();
                    return new ChatResponse(
                        userPrompt,
                        answer,
                        thinking,
                        new ArrayList<>(),
                        llmModel,
                        0,
                        new Date(),
                        new Date());
                });
    }

    private PromptWithSearchResults getUserPromptWithRagAugmentation(final ChatRequest chatRequest) {
        return new PromptWithSearchResults(chatRequest.userPrompt(), List.of());
    }

    private QuestionAnswerAdvisor questionAnswerAdvisor(
        final VectorStore vectorStore,
        final int topK,
        List<Integer> documentSourceIds
    ) {
        final String filterExpression = documentSourceIds.stream()
            .map("source_id == %s"::formatted)
            .collect(Collectors.joining(" || "));

        log.debug("filter expression: '{}'", filterExpression);
        log.debug("topK: '{}'", topK);

        return QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(
                SearchRequest.builder()
                    .topK(topK)
                    .filterExpression((filterExpression))
                    .build())
            .build();
    }
}

record PromptWithSearchResults(String userPrompt, List<VectorSearchResult> searchResults) {}
