package com.wininger.spring_ai_demo.conversations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RagDocumentCaptureAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {
  private final Logger log = LoggerFactory.getLogger(RagDocumentCaptureAdvisor.class);
  private final AtomicReference<List<Document>> capturedDocuments;

  public RagDocumentCaptureAdvisor(final AtomicReference<List<Document>> capturedDocuments) {
    this.capturedDocuments = capturedDocuments;
  }

  // for non-streaming calls
  @Override
  public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
    log.debug("processing aroundCall event to capture Rag documents");
    captureDocuments(advisedRequest);
    return chain.nextAroundCall(advisedRequest);
  }

  // for streaming calls
  @Override
  public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
    log.debug("processing aroundStream event to capture Rag documents");
    captureDocuments(advisedRequest);
    return chain.nextAroundStream(advisedRequest);
  }

  private void captureDocuments(AdvisedRequest advisedRequest) {
    Object docs = advisedRequest.adviseContext().get("qa_retrieved_documents");
    log.debug(docs != null ? "found RAG documents" : "no rag document found");
    if (docs instanceof List<?> docList) {
      log.info("found {} RAG documents", docList.size());
      capturedDocuments.set((List<Document>) docList);
    }
  }

  @Override
  public String getName() {
    return RagDocumentCaptureAdvisor.class.getSimpleName();
  }

  @Override
  public int getOrder() {
    return 10000;
  }
}
