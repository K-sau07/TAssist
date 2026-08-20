-- V4: chunks (pgvector). Embedding dim = 1024 per D5 (Voyage voyage-3.5).
CREATE TABLE chunk (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  file_id    UUID NOT NULL REFERENCES file(id) ON DELETE CASCADE,
  ordinal    INTEGER NOT NULL,
  text       TEXT NOT NULL,
  metadata   JSONB NOT NULL DEFAULT '{}'::jsonb,
  embedding  VECTOR(1024) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (file_id, ordinal)
);

CREATE INDEX ix_chunk_embedding ON chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX ix_chunk_file ON chunk(file_id);
