package com.wininger.spring_ai_demo.conversations;

import com.wininger.spring_ai_demo.api.ChatController;
import com.wininger.spring_ai_demo.api.ChatRequest;
import com.wininger.spring_ai_demo.api.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
public class ConversationService {
    @Value("${spring.ai.ollama.chat.options.model}")
    private String llmModel;

    private final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final AtomicInteger lastId = new AtomicInteger(0);

    private final ChatClient chatClient;


    public ConversationService(
        final ChatClient.Builder chatClientBuilder
    ) {
        this.chatClient = chatClientBuilder
            .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
            .defaultSystem("""
                You are a helpful assistant. You are confident in your answers. Your answers are short and to the point.
                If you do not know something you simply say so. Please do not explain your thinking, just answer the
                question.
                """)
            .build();
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

        final String modelResponse = chatClient
            .prompt()
            .user(chatRequest.userPrompt())
            .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId))
            .call()
            .content();

        log.info("finished processing /api/v1/chat {} at", new Date());

        return new ChatResponse(
            chatRequest.userPrompt(),
            cleanResponse(modelResponse),
            llmModel,
            conversationId,
            startTime
        );
    }

    private String cleanResponse(final String chatResponse) {
        if (isNull(chatResponse)) {
            return "";
        }

        final var thinkingAndResponding = splitThinking(chatResponse);

        log.info(thinkingAndResponding.toString());

        return thinkingAndResponding.response();
    }

    public ThinkingAndResponding splitThinking(final String chatResponse) {
        final int startOpenThinkTag = chatResponse.indexOf("<think>");

        if (startOpenThinkTag >= 0) {
            final int startCloseThinkTag = chatResponse.indexOf("</think>");

            final var thinking = chatResponse.substring(startOpenThinkTag + 7, startCloseThinkTag);
            final var response = chatResponse.substring(0, startOpenThinkTag) +
                chatResponse.substring(startCloseThinkTag + 8);

            return new ThinkingAndResponding(thinking.trim(), response.trim());
        } else {
            return new ThinkingAndResponding("", chatResponse.trim());
        }
    }
}
