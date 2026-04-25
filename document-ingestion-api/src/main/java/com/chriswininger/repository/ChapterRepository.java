package com.chriswininger.repository;

import com.chriswininger.db.generated.Tables;
import com.chriswininger.db.generated.tables.records.ChaptersRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChapterRepository {

    @Inject
    DSLContext dsl;

    public List<ChaptersRecord> findAll() {
        return dsl.selectFrom(Tables.CHAPTERS).fetch();
    }

    public List<ChaptersRecord> findByDocumentId(long documentId) {
        return dsl.selectFrom(Tables.CHAPTERS)
                .where(Tables.CHAPTERS.DOCUMENT_ID.eq(documentId))
                .fetch();
    }

    public Optional<ChaptersRecord> findById(long id) {
        return dsl.selectFrom(Tables.CHAPTERS)
                .where(Tables.CHAPTERS.ID.eq(id))
                .fetchOptional();
    }

    public ChaptersRecord insert(ChaptersRecord record) {
        return dsl.insertInto(Tables.CHAPTERS)
                .set(record)
                .returning()
                .fetchOne();
    }

    public int update(ChaptersRecord record) {
        return dsl.update(Tables.CHAPTERS)
                .set(record)
                .where(Tables.CHAPTERS.ID.eq(record.getId()))
                .execute();
    }

    public int deleteById(long id) {
        return dsl.deleteFrom(Tables.CHAPTERS)
                .where(Tables.CHAPTERS.ID.eq(id))
                .execute();
    }
}
