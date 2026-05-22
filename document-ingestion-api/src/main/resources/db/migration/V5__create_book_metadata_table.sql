CREATE TABLE book_metadata (
    id                              BIGSERIAL PRIMARY KEY,
    document_id                     BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    summary                         TEXT,
    title                           VARCHAR(255),
    author_name                     VARCHAR(255),
    publisher                       VARCHAR(255),
    year_published                  INTEGER,
    characters                      TEXT[],
    possible_questions_this_answers TEXT[],
    has_summary_information          BOOLEAN,
    full_text_front                 TEXT,
    full_text_back                  TEXT,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_book_metadata_document_id ON book_metadata(document_id);
