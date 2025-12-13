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
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.wininger.spring_ai_demo.api.chat.ChatStreamingResponseItemType.CONTENT;
import static com.wininger.spring_ai_demo.api.chat.ChatStreamingResponseItemType.THINKING;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
public class ConversationService {
    private static final String THINKING_START_TOKEN = "<think>";
    private static final String THINKING_END_TOKEN = "</think>";

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

    public ConversationService(
        final ChatClient.Builder chatClientBuilder,
        final VectorStore vectorStore,
        final VectorSearchService vectorSearchService,
        final TokenCountEstimator tokenCountEstimator
        ) {
        this.chatClient = chatClientBuilder
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(new InMemoryChatMemory()),
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
            .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId))
            .advisors(questionAnswerAdvisor(vectorStore, topK, chatRequest.documentSourceIds()))
            .advisors(new RagDocumentCaptureAdvisor(ragDocsRef));

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

        final String content = modelResponse
            .content();

        final List<Document> docs = nonNull(ragDocsRef.get()) ? ragDocsRef.get() : List.of();
        System.out.println("!!! rag: " + (docs != null ? docs.size() : 0));


        var endTime = new Date();
        log.info("finished processing /api/v1/chat {} at", endTime);

        final var thinkingAndResponding = cleanResponse(content);

        return new ChatResponse(
            chatRequest.userPrompt(),
            thinkingAndResponding.response(),
            thinkingAndResponding.thinking(),
            docs.stream().map(doc -> new VectorSearchResult(doc.getText(), doc.getMetadata(), doc.getScore())).toList(),
            llmModel,
            conversationId,
            startTime,
            endTime
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
            .advisors(new RagDocumentCaptureAdvisor(ragDocsRef));

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

        final AtomicBoolean isThinking = new AtomicBoolean(false);

        return prompt
            .stream()
            .content()
            .map(token -> {
                if (THINKING_START_TOKEN.equals(token)) {
                    isThinking.set(true);
                } else if (THINKING_END_TOKEN.equals(token)) {
                    isThinking.set(false);
                }

                final ChatStreamingResponseItemType responseItemType = isThinking.get() ? THINKING : CONTENT;

                return new ChatStreamingResponseItem(llmModel, conversationId, responseItemType, token);
            });
    }

    private ThinkingAndResponding cleanResponse(final String chatResponse) {
        if (isNull(chatResponse)) {
            return new ThinkingAndResponding("", "");
        }

        final var thinkingAndResponding = splitThinking(chatResponse);

        // log.info(thinkingAndResponding.toString());

        return thinkingAndResponding;
    }

    public ThinkingAndResponding splitThinking(final String chatResponse) {
        final int startOpenThinkTag = chatResponse.indexOf("<think>");

        if (startOpenThinkTag >= 0) {
            final int startCloseThinkTag = chatResponse.indexOf("</think>");

            if (startCloseThinkTag < 0) {
              log.warn("no closing tag");
              return new ThinkingAndResponding("", chatResponse.trim());
            }

            final var thinking = chatResponse.substring(startOpenThinkTag + 7, startCloseThinkTag);
            final var response = chatResponse.substring(0, startOpenThinkTag) +
                chatResponse.substring(startCloseThinkTag + 8);

            return new ThinkingAndResponding(thinking.trim(), response.trim());
        } else {
            return new ThinkingAndResponding("", chatResponse.trim());
        }
    }

    private PromptWithSearchResults getUserPromptWithRagAugmentation(final ChatRequest chatRequest) {
        return new PromptWithSearchResults(chatRequest.userPrompt(), List.of());
    }

    private QuestionAnswerAdvisor questionAnswerAdvisor(
        final VectorStore vectorStore,
        final int topK,
        List<Integer> documentSourceIds
    ) {
        final String filterExpression = nonNull(documentSourceIds) && !documentSourceIds.isEmpty()
            ? documentSourceIds.stream()
                .map("source_id == %s"::formatted)
                .collect(Collectors.joining(" || "))
            : null;

        log.debug("filter expression: '{}'", filterExpression);
        log.debug("topK: '{}'", topK);

        final var searchRequestBuilder = SearchRequest.builder().topK(topK);
        if (nonNull(filterExpression)) {
            searchRequestBuilder.filterExpression(filterExpression);
        }

        return QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(searchRequestBuilder.build())
            .build();
    }
}

record PromptWithSearchResults(String userPrompt, List<VectorSearchResult> searchResults) {}
