package com.wininger.spring_ai_demo;

import com.wininger.spring_ai_demo.conversations.ConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ConversationServiceTest {
  @Autowired
  public ConversationService conversationService;

  @Test
  public void testStringSplitting() {
    var test = "here is my <think>stuff and all that jazz</think> data.";

    var results = conversationService.splitThinking(test);

    System.out.println("!!! results: " + results);
  }

}
