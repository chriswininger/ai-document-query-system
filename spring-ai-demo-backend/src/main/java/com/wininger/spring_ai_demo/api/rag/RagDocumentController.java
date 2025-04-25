package com.wininger.spring_ai_demo.api.rag;

import com.wininger.spring_ai_demo.jooq.generated.tables.records.DocumentImportRecord;
import com.wininger.spring_ai_demo.rag.DocumentImportService;
import com.wininger.spring_ai_demo.rag.VectorStorageService;
import com.wininger.spring_ai_demo.rag.dto.DtoDocumentImport;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rag/document")
@CrossOrigin("*")
public class RagDocumentController {
  private final DocumentImportService documentImportService;
  private final VectorStorageService vectorStorageService;

  public RagDocumentController(
      final DocumentImportService documentImportService,
      final VectorStorageService vectorStorageService
  ) {
    this.documentImportService = documentImportService;
    this.vectorStorageService = vectorStorageService;
  }

  @GetMapping(path = "/imported/all", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<DtoDocumentImport> getAllImportedDocuments() {
    return documentImportService.getAllDocuments();
  }

  @PostMapping(path = "/process-and-store", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ProcessImportedDocumentResponse processAndStoreImportedDocuments(@RequestBody ProcessImportedDocumentRequest processImportedDocumentRequest) {
    var numProcessed = vectorStorageService.storeChunks(processImportedDocumentRequest.id());

    return new ProcessImportedDocumentResponse(numProcessed);
  }
}
