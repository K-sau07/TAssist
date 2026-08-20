-- V3: folders + files + folder_file join
CREATE TABLE folder (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id    UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  name        TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (owner_id, name)
);

CREATE TABLE file (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id          UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  original_filename TEXT NOT NULL,
  type              file_type NOT NULL,
  size_bytes        BIGINT NOT NULL,
  storage_key       TEXT NOT NULL,
  content_hash      TEXT NOT NULL,
  status            file_status NOT NULL DEFAULT 'UPLOADING',
  failure_reason    TEXT,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (owner_id, content_hash)
);
CREATE INDEX ix_file_owner ON file(owner_id);

CREATE TABLE folder_file (
  folder_id UUID NOT NULL REFERENCES folder(id) ON DELETE CASCADE,
  file_id   UUID NOT NULL REFERENCES file(id) ON DELETE CASCADE,
  added_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (folder_id, file_id)
);
CREATE INDEX ix_folder_file_file ON folder_file(file_id);
