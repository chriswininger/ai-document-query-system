package com.wininger.spring_ai_demo.config;

import com.wininger.spring_ai_demo.Tokenizer.DeepSeekTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenEstimatorConfig {

  @Bean
  public TokenCountEstimator tokenCountEstimator() {
    return new DeepSeekTokenCountEstimator();
  }
}
