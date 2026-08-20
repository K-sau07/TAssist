-- V5: spreadsheet structured mode. Summary embedding dim = 1024 per D5.
CREATE TABLE spreadsheet_sheet (
  id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  file_id                     UUID NOT NULL REFERENCES file(id) ON DELETE CASCADE,
  sheet_name                  TEXT NOT NULL,
  column_names                JSONB NOT NULL,
  column_types                JSONB NOT NULL,
  row_count                   BIGINT NOT NULL,
  schema_summary              TEXT NOT NULL,
  schema_summary_embedding    VECTOR(1024) NOT NULL,
  UNIQUE (file_id, sheet_name)
);
CREATE INDEX ix_sheet_summary_embedding
  ON spreadsheet_sheet USING ivfflat (schema_summary_embedding vector_cosine_ops) WITH (lists = 100);

CREATE TABLE spreadsheet_row (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sheet_id   UUID NOT NULL REFERENCES spreadsheet_sheet(id) ON DELETE CASCADE,
  row_number BIGINT NOT NULL,
  values     JSONB NOT NULL,
  UNIQUE (sheet_id, row_number)
);
CREATE INDEX ix_row_sheet ON spreadsheet_row(sheet_id);
CREATE INDEX ix_row_values ON spreadsheet_row USING gin (values jsonb_path_ops);
