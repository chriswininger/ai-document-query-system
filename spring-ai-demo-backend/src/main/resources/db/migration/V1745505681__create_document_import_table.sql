CREATE TABLE document_import(
  id SERIAL PRIMARY KEY,
  source_name VARCHAR(500) NOT NULL,
  content TEXT NOT NULL,
  metadata JSONB,
  status VARCHAR(50) NOT NULL ,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
)
