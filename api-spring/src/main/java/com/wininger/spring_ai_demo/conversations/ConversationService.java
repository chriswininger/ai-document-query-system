package com.wininger.spring_ai_demo.conversations;

import com.wininger.spring_ai_demo.api.chat.ChatRequest;
import com.wininger.spring_ai_demo.api.chat.ChatResponse;
import com.wininger.spring_ai_demo.api.chat.ChatStreamingResponseItem;
import com.wininger.spring_ai_demo.api.chat.ChatStreamingResponseItemType;
import com.wininger.spring_ai_demo.api.rag.VectorSearchResult;
import com.wininger.spring_ai_demo.api.rag.VectorSearchService;
import com.wininger.spring_ai_demo.logging.LoggingAdvisor;
import com.wininger.spring_ai_demo.rag.QueryRewritingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
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

    private final QueryRewritingService queryRewritingService;

    public ConversationService(
        final ChatClient.Builder chatClientBuilder,
        final VectorStore vectorStore,
        final VectorSearchService vectorSearchService,
        final TokenCountEstimator tokenCountEstimator,
        final OllamaChatModel ollamaChatModel,
        final QueryRewritingService queryRewritingService,
        final LoggingAdvisor loggingAdvisor
    ) {
        this.ollamaChatModel = ollamaChatModel;
        this.queryRewritingService = queryRewritingService;

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
                loggingAdvisor
            )
            .defaultSystem("""
                You are a helpful assistant. If you do not know something you simply say so.
                """)
            .build();

        this.tokenCountEstimator = tokenCountEstimator;
        this.vectorStore = vectorStore;
        this.vectorSearchService = vectorSearchService;
    }

    public final Integer getNextConversationId() {
        return lastId.addAndGet(1);
    }

    public ChatResponse performConversationExchange(
        final ChatRequest chatRequest
    ) {
        final var startTime = new Date();

        // TODO: Thinking will be missing until the spring streaming bug is fixed
        final var thinkingBuilder = new StringBuilder();
        final var outputBuilder = new StringBuilder();
        final List<VectorSearchResult> ragDocs = new ArrayList<>();

        final List<ChatStreamingResponseItem> streamingResponseItems = performConversationExchangeStreaming(chatRequest)
            .collectList()
            .block();

        if (nonNull(streamingResponseItems)) {
            streamingResponseItems.forEach(item -> {
                switch (item.itemType()) {
                    case THINKING -> thinkingBuilder.append(item.output());
                    case CONTENT -> outputBuilder.append(item.output());
                    case RAG_DOCUMENT -> ragDocs.add(item.vectorSearchResult());
                }
            });
        }

        final int conversationId = streamingResponseItems.isEmpty()
            ? nonNull(chatRequest.conversationId()) ? chatRequest.conversationId() : getNextConversationId()
            : streamingResponseItems.getFirst().conversationId();

        return new ChatResponse(
            chatRequest.userPrompt(),
            outputBuilder.toString(),
            thinkingBuilder.toString(),
            ragDocs,
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
        final int conversationId = nonNull(chatRequest.conversationId())
            ? chatRequest.conversationId()
            : getNextConversationId();

        final int topK = chatRequest.numberOfRagDocumentsToInclude() != null
            ? chatRequest.numberOfRagDocumentsToInclude()
            : 5;

        // Track the list of rag documents returned by the pipeline so that we can stream them to the client
        final AtomicReference<List<Document>> ragDocsRef = new AtomicReference<>();
        final ChatClientRequestSpec prompt = chatClient
            .prompt()
            .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId));

        // if the user has enabled RAG
        if (topK > 0 && !chatRequest.documentSourceIds().isEmpty()) {
            prompt.advisors(QueryRewritingVectorStoreAdvisor.builder(vectorSearchService)
                .queryRewritingService(queryRewritingService)
                .topK(topK)
                .documentSourceIds(chatRequest.documentSourceIds())
                .build());

            // this is just used so that we can track the rag documents returned
            prompt.advisors(new RagDocumentCaptureAdvisor(ragDocsRef)); // TODO: I think I can actually get these from modelResponse object
        }

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

                final var promptTokens = chatResponse.getMetadata().getUsage().getPromptTokens();
                final var completionTokens = chatResponse.getMetadata().getUsage().getCompletionTokens();
                final var totalTokens = chatResponse.getMetadata().getUsage().getTotalTokens();

                // these will be 0 as we stream, but will be populated when the final item comes in
                if (totalTokens > 0) {
                    log.info("conversationID: {} -> promptTokens: {}, completionTokens: {}, totalTokens: {}",
                        conversationId, promptTokens, completionTokens, totalTokens);
                }

                return new ChatStreamingResponseItem(llmModel, conversationId, itemType, text, null);
            })
            .concatWith(Flux.defer(() -> {
                // possibly we could get this from the chatResponse map qa_retrieved_documents and avoid the need
                // for using the advisor to capture them, but this doesn't seem to be populated in my streaming version,
                // so perhaps we'll keep using the advisor to populate them for now
                log.debug("Concat documents to stream");
                final List<Document> docs = nonNull(ragDocsRef.get()) ? ragDocsRef.get() : List.of();
                return Flux.fromIterable(docs)
                    .map(doc -> new ChatStreamingResponseItem(
                        llmModel,
                        conversationId,
                        RAG_DOCUMENT,
                        doc.getText(),
                        new VectorSearchResult(doc.getText(), doc.getMetadata(), doc.getScore())
                    ));
            }));
    }

    private PromptWithSearchResults getUserPromptWithRagAugmentation(final ChatRequest chatRequest) {
        return new PromptWithSearchResults(chatRequest.userPrompt(), List.of());
    }

    private QuestionAnswerAdvisor questionAnswerAdvisor(
        final VectorStore vectorStore,
        final int topK,
        List<Integer> documentSourceIds
    ) {
        final String filterExpression = buildFilterExpression(documentSourceIds);

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

    private String buildFilterExpression(List<Integer> documentSourceIds) {
        return documentSourceIds.stream()
            .map("source_id == %s"::formatted)
            .collect(Collectors.joining(" || "));
    }
}

record PromptWithSearchResults(String userPrompt, List<VectorSearchResult> searchResults) {}
