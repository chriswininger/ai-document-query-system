ALTER TABLE document_import_chunk
DROP CONSTRAINT IF EXISTS document_import_chunk_document_import_id_fkey;

ALTER TABLE document_import_chunk
    ADD CONSTRAINT document_import_chunk_document_import_id_fkey
    FOREIGN KEY (document_import_id)
    REFERENCES document_import(id)
    ON DELETE CASCADE;
