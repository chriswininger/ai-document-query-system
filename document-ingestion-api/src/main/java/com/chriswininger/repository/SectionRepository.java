package com.chriswininger.repository;

import com.chriswininger.db.generated.Tables;
import com.chriswininger.db.generated.tables.records.SectionsRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SectionRepository {

    @Inject
    DSLContext dsl;

    public List<SectionsRecord> findAll() {
        return dsl.selectFrom(Tables.SECTIONS).fetch();
    }

    public List<SectionsRecord> findByChapterId(long chapterId) {
        return dsl.selectFrom(Tables.SECTIONS)
                .where(Tables.SECTIONS.CHAPTER_ID.eq(chapterId))
                .fetch();
    }

    public Optional<SectionsRecord> findById(long id) {
        return dsl.selectFrom(Tables.SECTIONS)
                .where(Tables.SECTIONS.ID.eq(id))
                .fetchOptional();
    }

    public SectionsRecord insert(SectionsRecord record) {
        return dsl.insertInto(Tables.SECTIONS)
                .set(record)
                .returning()
                .fetchOne();
    }

    public int update(SectionsRecord record) {
        return dsl.update(Tables.SECTIONS)
                .set(record)
                .where(Tables.SECTIONS.ID.eq(record.getId()))
                .execute();
    }

    public int deleteById(long id) {
        return dsl.deleteFrom(Tables.SECTIONS)
                .where(Tables.SECTIONS.ID.eq(id))
                .execute();
    }
}
