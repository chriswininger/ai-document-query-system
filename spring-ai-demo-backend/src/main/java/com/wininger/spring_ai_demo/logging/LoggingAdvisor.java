package com.wininger.spring_ai_demo.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

public class LoggingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {
  private static final Logger logger = LoggerFactory.getLogger(LoggingAdvisor.class);

  @Override
  public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
    logger.info("Before: {}", advisedRequest);

    final var advisorResponse = chain.nextAroundCall(advisedRequest);

    logger.info("AFTER: {}", advisorResponse);

    return advisorResponse;
  }

  @Override
  public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
    logger.debug("BEFORE: {}", advisedRequest);

    Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advisedRequest);

    return new MessageAggregator().aggregateAdvisedResponse(advisedResponses,
        advisedResponse -> logger.debug("AFTER: {}", advisedResponse));
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public int getOrder() {
    return 0;
  }
}
