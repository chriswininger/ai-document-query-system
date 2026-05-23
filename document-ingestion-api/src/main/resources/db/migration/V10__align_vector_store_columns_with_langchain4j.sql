/* [jooq ignore start] */
-- LangChain4j PgVectorEmbeddingStore expects embedding_id and text, not id and content.
ALTER TABLE vector_store RENAME COLUMN id TO embedding_id;
ALTER TABLE vector_store RENAME COLUMN content TO text;
/* [jooq ignore stop] */
