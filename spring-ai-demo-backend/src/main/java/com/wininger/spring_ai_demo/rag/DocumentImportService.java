package com.wininger.spring_ai_demo.rag;

import com.wininger.spring_ai_demo.jooq.generated.tables.DocumentImport;
import com.wininger.spring_ai_demo.jooq.generated.tables.DocumentImportChunk;
import com.wininger.spring_ai_demo.jooq.generated.tables.records.DocumentImportRecord;
import com.wininger.spring_ai_demo.rag.dto.DtoDocumentImport;
import com.wininger.spring_ai_demo.rag.dto.DtoDocumentImportChunk;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.wininger.spring_ai_demo.jooq.generated.Tables.DOCUMENT_IMPORT;
import static com.wininger.spring_ai_demo.jooq.generated.tables.DocumentImportChunk.DOCUMENT_IMPORT_CHUNK;

@Service
public class DocumentImportService {
  private final DSLContext dsl;

  public DocumentImportService(DSLContext dsl) {
    this.dsl = dsl;
  }

  public List<DtoDocumentImport> getAllDocuments() {
    return dsl.selectFrom(DocumentImport.DOCUMENT_IMPORT)
        .fetch()
        .map(row -> new DtoDocumentImport(
            row.get(DOCUMENT_IMPORT.ID),
            row.get(DOCUMENT_IMPORT.SOURCE_NAME),
            row.get(DOCUMENT_IMPORT.NON_CHUNKED_CONTENT),
            row.get(DOCUMENT_IMPORT.METADATA),
            row.get(DOCUMENT_IMPORT.CREATED_AT),
            row.get(DOCUMENT_IMPORT.UPDATED_AT)
        ));
  }

  public List<DtoDocumentImportChunk> getAllDocumentChunksByDocId(final int docId) {
    return dsl.selectFrom(DOCUMENT_IMPORT_CHUNK)
        .where(DOCUMENT_IMPORT_CHUNK.DOCUMENT_IMPORT_ID.eq(docId))
        .fetch()
        .map(row -> new DtoDocumentImportChunk(
            row.get(DOCUMENT_IMPORT_CHUNK.ID),
            row.get(DOCUMENT_IMPORT_CHUNK.DOCUMENT_IMPORT_ID),
            row.get(DOCUMENT_IMPORT_CHUNK.SOURCE_NAME),
            row.get(DOCUMENT_IMPORT_CHUNK.CONTENT),
            row.get(DOCUMENT_IMPORT_CHUNK.METADATA),
            row.get(DOCUMENT_IMPORT_CHUNK.STATUS),
            row.get(DOCUMENT_IMPORT_CHUNK.CREATED_AT),
            row.get(DOCUMENT_IMPORT_CHUNK.UPDATED_AT)
        ));
  }
}
