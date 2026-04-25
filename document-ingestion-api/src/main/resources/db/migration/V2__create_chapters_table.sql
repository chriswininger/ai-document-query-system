CREATE TABLE chapters (
    id                          BIGSERIAL PRIMARY KEY,
    document_id                 BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    summary                     TEXT,
    characters                  TEXT[],
    full_text                   TEXT,
    possible_questions_this_answers TEXT[],
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chapters_document_id ON chapters(document_id);
