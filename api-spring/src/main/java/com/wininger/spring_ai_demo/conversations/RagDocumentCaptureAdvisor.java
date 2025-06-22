package com.wininger.spring_ai_demo.conversations;

import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RagDocumentCaptureAdvisor implements CallAroundAdvisor {
  private final AtomicReference<List<Document>> capturedDocuments;

  public RagDocumentCaptureAdvisor(final AtomicReference<List<Document>> capturedDocuments) {
    this.capturedDocuments = capturedDocuments;
  }

  @Override
  public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
    Object docs = advisedRequest.adviseContext().get("qa_retrieved_documents");
    if (docs instanceof List<?> docList) {
      capturedDocuments.set((List<Document>) docList);
    }

    return chain.nextAroundCall(advisedRequest);
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
