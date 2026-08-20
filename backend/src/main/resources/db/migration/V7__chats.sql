-- V7: chats + messages
CREATE TABLE chat (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id    UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  scope       chat_scope NOT NULL,
  folder_id   UUID REFERENCES folder(id) ON DELETE SET NULL,
  channel_id  UUID REFERENCES channel(id) ON DELETE CASCADE,
  title       TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (
    (scope = 'REGULAR' AND folder_id IS NULL AND channel_id IS NULL) OR
    (scope = 'FOLDER'  AND folder_id IS NOT NULL AND channel_id IS NULL) OR
    (scope = 'CHANNEL' AND channel_id IS NOT NULL AND folder_id IS NULL)
  )
);
CREATE INDEX ix_chat_owner   ON chat(owner_id, updated_at DESC);
CREATE INDEX ix_chat_folder  ON chat(folder_id);
CREATE INDEX ix_chat_channel ON chat(channel_id);

CREATE TABLE message (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  chat_id          UUID NOT NULL REFERENCES chat(id) ON DELETE CASCADE,
  role             message_role NOT NULL,
  content          TEXT NOT NULL,
  citations        JSONB NOT NULL DEFAULT '[]'::jsonb,
  mentioned_files  JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_message_chat ON message(chat_id, created_at ASC);
