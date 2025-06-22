package com.wininger.spring_ai_demo.api.rag;

import com.wininger.spring_ai_demo.rag.db.DocumentImportService;
import com.wininger.spring_ai_demo.rag.VectorProcessingService;
import com.wininger.spring_ai_demo.rag.db.VectorStoreService;
import com.wininger.spring_ai_demo.rag.dto.DtoDocumentImport;
import com.wininger.spring_ai_demo.rag.dto.DtoVectorStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rag/document")
@CrossOrigin("*")
public class RagDocumentController {
  private final DocumentImportService documentImportService;
  private final VectorProcessingService vectorProcessingService;
  private final VectorStoreService vectorStoreService;

  public RagDocumentController(
      final DocumentImportService documentImportService,
      final VectorProcessingService vectorProcessingService,
      final VectorStoreService vectorStoreService
  ) {
    this.documentImportService = documentImportService;
    this.vectorProcessingService = vectorProcessingService;
    this.vectorStoreService = vectorStoreService;
  }

  @GetMapping(path = "/imported/all", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<DtoDocumentImport> getAllImportedDocuments() {
    return documentImportService.getAllDocuments();
  }

  @PostMapping(path = "/process-and-store", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ProcessImportedDocumentResponse processAndStoreImportedDocuments(@RequestBody ProcessImportedDocumentRequest processImportedDocumentRequest) {
    var numProcessed = vectorProcessingService.storeChunks(processImportedDocumentRequest.id());

    return new ProcessImportedDocumentResponse(numProcessed);
  }

  @GetMapping(path = "/vectors/document/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<DtoVectorStore> getAllVectorsByDocumentId(@PathVariable Integer id) {
    return vectorStoreService.getAllVectorsByDocumentId(id);
  }
}
