-- V8: notes + todos + quota_usage
CREATE TABLE note (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id   UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  content    TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_note_owner ON note(owner_id);

CREATE TABLE todo_item (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id   UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  text       TEXT NOT NULL,
  done       BOOLEAN NOT NULL DEFAULT FALSE,
  position   INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_todo_owner ON todo_item(owner_id, position);

CREATE TABLE quota_usage (
  user_id           UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  period            DATE NOT NULL,
  questions_asked   BIGINT NOT NULL DEFAULT 0,
  files_uploaded    BIGINT NOT NULL DEFAULT 0,
  bytes_stored      BIGINT NOT NULL DEFAULT 0,
  tokens_consumed   BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id, period)
);
