package com.wininger.spring_ai_demo.api.chat;

import com.wininger.spring_ai_demo.conversations.ConversationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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

    @PostMapping(path = "/generic/test", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chatTest(@RequestBody final ChatRequest chatRequest) {
        return this.conversationService.performTestChat(chatRequest.userPrompt());
    }


    @PostMapping(path = "/generic/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Flux<ChatStreamingResponseItem>> chatStreaming(@RequestBody final ChatRequest chatRequest) {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(this.conversationService.performConversationExchangeStreaming(chatRequest));
    }
}
