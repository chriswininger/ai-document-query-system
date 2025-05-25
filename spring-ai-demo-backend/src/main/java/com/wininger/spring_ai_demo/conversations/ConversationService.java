package com.wininger.spring_ai_demo.conversations;

import com.wininger.spring_ai_demo.api.chat.ChatRequest;
import com.wininger.spring_ai_demo.api.chat.ChatResponse;
import com.wininger.spring_ai_demo.api.rag.VectorSearchResult;
import com.wininger.spring_ai_demo.api.rag.VectorSearchService;
import com.wininger.spring_ai_demo.logging.LoggingAdvisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
public class ConversationService {
    @Value("${spring.ai.ollama.chat.options.model}")
    private String llmModel;

    @Value("${spring.ai.ollama.chat.options.num-ctx}")
    private int contextWindow;

    private final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final AtomicInteger lastId = new AtomicInteger(0);

    private final ChatClient chatClient;

    private final VectorSearchService vectorSearchService;

    private final TokenCountEstimator tokenCountEstimator;

    public ConversationService(
        final ChatClient.Builder chatClientBuilder,
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

        final ChatClientRequestSpec prompt = chatClient
            .prompt()
            .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId));

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

        var endTime = new Date();
        log.info("finished processing /api/v1/chat {} at", endTime);

        final var thinkingAndResponding = cleanResponse(content);

        return new ChatResponse(
            chatRequest.userPrompt(),
            thinkingAndResponding.response(),
            thinkingAndResponding.thinking(),
            userPromptInfo.searchResults(),
            llmModel,
            conversationId,
            startTime,
            endTime
        );
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
        final int topK = chatRequest.numberOfRagDocumentsToInclude() != null
            ? chatRequest.numberOfRagDocumentsToInclude()
            : 5;

        if (nonNull(chatRequest.documentSourceIds()) && !chatRequest.documentSourceIds().isEmpty()) {
            log.info("Using Rag -- topK: {}", topK);

            final var vectorSearchResults = vectorSearchService.performSearch(
                chatRequest.userPrompt(), topK, chatRequest.documentSourceIds());

            // sort the docs so that they come in the sequence they were included in the source material
            final var docs = vectorSearchResults.stream()
                .sorted((a, b) -> {
                    return (int)a.metadata().get("chunk_id") - (int)b.metadata().get("chunk_id");
                })
                .map(VectorSearchResult::text)
                .collect(Collectors.joining("\n"));

            final var userPrompt = """
        Answer the question that comes at the end of this dialog, based only on the information between the Info tags:
        
        <Info>
        %s
        </Info>
        
        <Question>
        %s
        </Question>
        """.formatted(docs, chatRequest.userPrompt());

            return new PromptWithSearchResults(userPrompt, vectorSearchResults);
        }

        return new PromptWithSearchResults(chatRequest.userPrompt(), List.of());
    }
}

record PromptWithSearchResults(String userPrompt, List<VectorSearchResult> searchResults) {}
