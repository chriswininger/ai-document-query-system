package com.wininger.spring_ai_demo.config;

import io.micrometer.common.lang.NonNull;
import org.springframework.ai.ollama.api.ThinkOption;
import org.springframework.core.convert.converter.Converter;

// https://github.com/spring-projects/spring-ai/issues/4853
// https://github.com/spring-projects/spring-ai/pull/4854/files
public class ThinkOptionConverter implements Converter<String, ThinkOption> {
  @Override
  public ThinkOption convert(@NonNull String source) {
    return switch (source) {
      case "enabled" -> ThinkOption.ThinkBoolean.ENABLED;
      case "disabled" -> ThinkOption.ThinkBoolean.DISABLED;
      case "low" -> ThinkOption.ThinkLevel.LOW;
      case "medium" -> ThinkOption.ThinkLevel.MEDIUM;
      case "high" -> ThinkOption.ThinkLevel.HIGH;
      default -> throw new IllegalStateException("Unexpected think option value: " + source);
    };
  }
}

