package com.wininger.spring_ai_demo.api.chat;

import com.wininger.spring_ai_demo.conversations.ConversationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping(path = "/generic", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chat(@RequestBody final ChatRequest chatRequest) {
        return this.conversationService.performConversationExchange(chatRequest);
    }
}
