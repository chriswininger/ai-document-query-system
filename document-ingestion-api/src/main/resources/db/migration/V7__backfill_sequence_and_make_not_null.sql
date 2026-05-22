/* [jooq ignore start] */
UPDATE chapters
SET sequence = sub.row_num
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY document_id ORDER BY id ASC) - 1 AS row_num
    FROM chapters
) AS sub
WHERE chapters.id = sub.id;

UPDATE sections
SET sequence = sub.row_num
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY chapter_id ORDER BY id ASC) - 1 AS row_num
    FROM sections
) AS sub
WHERE sections.id = sub.id;
/* [jooq ignore stop] */
