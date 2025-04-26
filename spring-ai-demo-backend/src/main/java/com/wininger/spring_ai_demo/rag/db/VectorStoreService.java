package com.wininger.spring_ai_demo.rag.db;

import com.wininger.spring_ai_demo.jooq.generated.tables.VectorStore;
import com.wininger.spring_ai_demo.rag.dto.DtoVectorStore;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.wininger.spring_ai_demo.jooq.generated.tables.VectorStore.VECTOR_STORE;
import static org.jooq.impl.DSL.*;

@Service
public class VectorStoreService {
  private final DSLContext dsl;

  public VectorStoreService(DSLContext dsl) {
    this.dsl = dsl;
  }

  public List<DtoVectorStore> getAllVectorsByDocumentId(final int docId) {
    return dsl.selectFrom(VECTOR_STORE)
        .where(cast(jsonGetAttributeAsText(VECTOR_STORE.METADATA, "source_id"), Integer.class).eq(docId))
        .fetch()
        .map(row -> new DtoVectorStore(
            row.get(VECTOR_STORE.ID),
            row.get(VECTOR_STORE.CONTENT),
            row.get(VECTOR_STORE.METADATA),
           null
        ));
  }

  public void deleteAllVectorsByDocumentId(final int docId) {
    dsl.deleteFrom(VECTOR_STORE)
        .where(cast(jsonGetAttributeAsText(VECTOR_STORE.METADATA, "source_id"), Integer.class).eq(docId))
        .execute();
  }
}
