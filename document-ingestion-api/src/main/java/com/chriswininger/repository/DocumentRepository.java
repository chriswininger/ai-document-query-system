package com.chriswininger.repository;

import com.chriswininger.db.generated.Tables;
import com.chriswininger.db.generated.tables.records.DocumentsRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DocumentRepository {

    @Inject
    DSLContext dsl;

    public List<DocumentsRecord> findAll() {
        return dsl.selectFrom(Tables.DOCUMENTS).fetch();
    }

    public Optional<DocumentsRecord> findById(long id) {
        return dsl.selectFrom(Tables.DOCUMENTS)
                .where(Tables.DOCUMENTS.ID.eq(id))
                .fetchOptional();
    }

    public DocumentsRecord insert(DocumentsRecord record) {
        return dsl.insertInto(Tables.DOCUMENTS)
                .set(record)
                .returning()
                .fetchOne();
    }

    public int update(DocumentsRecord record) {
        return dsl.update(Tables.DOCUMENTS)
                .set(record)
                .where(Tables.DOCUMENTS.ID.eq(record.getId()))
                .execute();
    }

    public int deleteById(long id) {
        return dsl.deleteFrom(Tables.DOCUMENTS)
                .where(Tables.DOCUMENTS.ID.eq(id))
                .execute();
    }
}
