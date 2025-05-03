package com.wininger.spring_ai_demo.conversations;

import com.wininger.spring_ai_demo.api.chat.ChatRequest;
import com.wininger.spring_ai_demo.api.chat.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
public class ConversationService {
    @Value("${spring.ai.ollama.chat.options.model}")
    private String llmModel;

    private final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final AtomicInteger lastId = new AtomicInteger(0);

    private final ChatClient chatClient;

    private final VectorStore vectorStore;


    public ConversationService(
        final ChatClient.Builder chatClientBuilder,
        final VectorStore vectorStore
    ) {
        this.chatClient = chatClientBuilder
            .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
            .defaultSystem("""
                You are a helpful assistant. You are confident in your answers. Your answers are short and to the point.
                If you do not know something you simply say so. Please do not explain your thinking, just answer the
                question.
                """)
            .build();

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

        final ChatClientRequestSpec prompt = chatClient
            .prompt()
            .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId));

        prompt.user(chatRequest.userPrompt());

        if (nonNull(chatRequest.systemPrompt())) {
          prompt.system(chatRequest.systemPrompt());
        }


        final String modelResponse = prompt
            .call()
            .content();

        var endTime = new Date();
        log.info("finished processing /api/v1/chat {} at", endTime);

        return new ChatResponse(
            chatRequest.userPrompt(),
            cleanResponse(modelResponse),
            llmModel,
            conversationId,
            startTime,
            endTime
        );
    }

    public ChatResponse performConversationExchangeWithJack(
        final ChatRequest chatRequest
    ) {
        final var startTime = new Date();
        final int conversationId = nonNull(chatRequest.conversationId())
            ? chatRequest.conversationId()
            : getNextConversationId();

        log.info("processing /api/v1/chat/with-jack at {}", startTime);

        final ChatClientRequestSpec prompt = chatClient
            .prompt()
            .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId));

        //final var query = queryForTheVectorDb(chatRequest.userPrompt());

        //log.info("Query to perform similarity search: '{}'", query);

        final var searchRequest = SearchRequest.builder()
            .topK(30)
            .filterExpression("source_id == 6") // 6 is source_id for Master_AND_Commander
            .query(chatRequest.userPrompt())
            .build();

        final var captainsLogs = vectorStore.similaritySearch(searchRequest).stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n"));

        log.info("captains logs, found: {} entries", captainsLogs.length());

        final var systemPrompt = """
        You are a helpful member of Captain Jack Aubrey's crew aboard the Sloop named HMS Sophie.
        
        You will respond the dialog of the time period. Here are some examples:
        
        ```
        “But you know as well as I, patriotism is a word; and one that generally comes to mean either my country, right or wrong, which is infamous, or my country is always right, which is imbecile.”
    
        “My dear creature, I have done with all debate. But you know as well as I, patriotism is a word; and one that generally comes to mean either MY COUNTRY, RIGHT OR WRONG, which is infamous, or MY COUNTRY IS ALWAYS RIGHT, which is imbecile.”
      
        “Patriotism is a word; and one that generally comes to mean either my country, right or wrong, which is infamous, or my country is always right, which is imbecile.”    
        ```
        """.formatted(captainsLogs);

        final var userPrompt = """
        Answer the question that comes at the end of this dialog, based only on the information between the Info tags:
        
        <Info>
        %s
        </Info>
        
        <Question>
        %s
        </Question>
        """.formatted(captainsLogs, chatRequest.userPrompt());

        log.info("system prompt: \n======\n{}\n=======\n", systemPrompt);
        log.info("user prompt: \n======\n{}\n=======\n", userPrompt);

        prompt.system(systemPrompt);
        prompt.user(userPrompt);


        final String modelResponse = prompt
            .call()
            .content();

        var endTime = new Date();
        log.info("finished processing /api/v1/chat/with-jack {} at", endTime);

        return new ChatResponse(
            chatRequest.userPrompt(),
            cleanResponse(modelResponse),
            llmModel,
            conversationId,
            startTime,
            endTime
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

    private String queryForTheVectorDb(final String originalPrompt) {
        log.info("get vector db query for: '{}'", originalPrompt);
        final ChatClientRequestSpec prompt = chatClient
            .prompt()
            .system("""
            Your purpose is to provide good query strings to match related documents in a vector database. When prompted you will respond with a query string good for doing  similarity search
            
            For Example, you see:
            
            "Tell you what you can about Captain Jack Aubrey",
            
            you respond:
            
            "Jack Aubrey"
            """)
            .user(originalPrompt);

        return splitThinking(prompt.call().content()).response();
    }
}
