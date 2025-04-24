CREATE TABLE IF NOT EXISTS document_import(
    id SERIAL PRIMARY KEY,
    source_name VARCHAR(500) NOT NULL,
    non_chunked_content text NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS document_import_chunk(
  id SERIAL PRIMARY KEY,
  document_import_id INTEGER NOT NULL REFERENCES document_import(id),
  -- this is redundant, but let's keep it for now
  source_name VARCHAR(500) NOT NULL,
  content TEXT NOT NULL,
  metadata JSONB,
  status VARCHAR(50) NOT NULL ,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
)
