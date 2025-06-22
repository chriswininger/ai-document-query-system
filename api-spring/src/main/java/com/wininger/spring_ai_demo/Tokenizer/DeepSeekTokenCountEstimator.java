package com.wininger.spring_ai_demo.Tokenizer;

import jdk.jshell.spi.ExecutionControl;
import org.springframework.ai.content.MediaContent;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekTokenCountEstimator implements TokenCountEstimator {

  @Value("${spring.ai.ollama.chat.options.model}")
  private String modelName;

  @Override
  public int estimate(String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }

    // https://api-docs.deepseek.com/quick_start/token_usage
    // "1 English character ≈ 0.3 token."
    return Math.round(text.length() * .3f);
  }

  @Override
  public int estimate(MediaContent content) {
    throw new RuntimeException("'estimate' has not been implemented for Media Content");
  }

  @Override
  public int estimate(Iterable<MediaContent> messages) {
    int total = 0;

    for (var message : messages) {
      total += this.estimate(message.getText());
    }

    return total;
  }
}

