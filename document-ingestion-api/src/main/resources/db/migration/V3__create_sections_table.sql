CREATE TABLE sections (
    id                          BIGSERIAL PRIMARY KEY,
    chapter_id                  BIGINT NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    summary                     TEXT,
    characters                  TEXT[],
    full_text                   TEXT,
    possible_questions_this_answers TEXT[],
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sections_chapter_id ON sections(chapter_id);
