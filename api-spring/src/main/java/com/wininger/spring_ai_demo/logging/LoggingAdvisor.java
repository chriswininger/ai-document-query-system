package com.wininger.spring_ai_demo.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class LoggingAdvisor implements CallAdvisor, StreamAdvisor {
  private static final Logger logger = LoggerFactory.getLogger(LoggingAdvisor.class);
  
  private final boolean enabled;
  
  public LoggingAdvisor(
      @Value("${spring.ai.chat.logging-advisor.enabled:false}") boolean enabled
  ) {
    this.enabled = enabled;
  }

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    if (!enabled) {
      return chain.nextCall(request);
    }
    
    logger.info("""

      Request -- prompt:
      =========================
      {}
      ========================
      """, request.prompt());

    logger.info("""
       Request -- context:
       =======================
       {}
       =======================
    """, request.context());

    logger.info("""

        Request -- messages:
        ====================
        {}
        ====================
        """, request.prompt().getInstructions());


    final var advisorResponse = chain.nextCall(request);

    logger.info("""
        Result:
        ================
        {}
        ================
        """, advisorResponse.chatResponse().getResult());

    logger.info("Token Information: {}",
        advisorResponse.chatResponse() != null ? advisorResponse.chatResponse().getMetadata().getUsage() : null);

    return advisorResponse;
  }

  @Override
  public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
    if (!enabled) {
      return chain.nextStream(request);
    }
    
    logger.debug("BEFORE: {}", request);

    Flux<ChatClientResponse> responses = chain.nextStream(request);

    return responses.doOnNext(response -> logger.debug("AFTER: {}", response));
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public int getOrder() {
    return 10001;
  }
}
