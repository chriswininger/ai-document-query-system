ALTER TABLE documents
    ADD COLUMN year_published                   INTEGER,
    ADD COLUMN author_name                      VARCHAR(255),
    ADD COLUMN possible_questions_this_answers   TEXT[];
