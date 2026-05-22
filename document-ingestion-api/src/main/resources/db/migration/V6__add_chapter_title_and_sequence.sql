ALTER TABLE chapters
    ADD COLUMN chapter_title VARCHAR(255),
    ADD COLUMN sequence      INTEGER;

ALTER TABLE sections
    ADD COLUMN sequence INTEGER;
