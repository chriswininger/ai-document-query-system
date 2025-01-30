package com.wininger.spring_ai_demo.api;

import com.wininger.spring_ai_demo.conversations.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

import static java.util.Objects.nonNull;

@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin("*")
public class ChatController {
    private final ConversationService conversationService;

    public ChatController(
            final ConversationService conversationService
    ) {
        this.conversationService = conversationService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chat(@RequestBody final ChatRequest chatRequest) {
        return this.conversationService.performConversationExchange(chatRequest);
    }
}
