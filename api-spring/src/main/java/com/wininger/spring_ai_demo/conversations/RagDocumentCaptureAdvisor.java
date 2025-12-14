package com.wininger.spring_ai_demo.conversations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RagDocumentCaptureAdvisor implements CallAdvisor, StreamAdvisor {
  private final Logger log = LoggerFactory.getLogger(RagDocumentCaptureAdvisor.class);
  private final AtomicReference<List<Document>> capturedDocuments;

  public RagDocumentCaptureAdvisor(final AtomicReference<List<Document>> capturedDocuments) {
    this.capturedDocuments = capturedDocuments;
  }

  // for non-streaming calls
  @Override
  public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    log.debug("processing adviseCall event to capture Rag documents");
    captureDocuments(request);
    return chain.nextCall(request);
  }

  // for streaming calls
  @Override
  public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
    log.debug("processing adviseStream event to capture Rag documents");
    captureDocuments(request);
    return chain.nextStream(request);
  }

  private void captureDocuments(ChatClientRequest request) {
    Object docs = request.context().get("qa_retrieved_documents");
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
