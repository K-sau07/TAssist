-- V6: channels + channel_file + membership
CREATE TABLE channel (
  id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id                     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  username                     CITEXT UNIQUE NOT NULL CHECK (username ~ '^[a-z0-9-]{3,32}$'),
  display_name                 TEXT NOT NULL,
  description                  TEXT NOT NULL DEFAULT '',
  expectation_summary          TEXT NOT NULL DEFAULT '',
  visibility                   channel_visibility NOT NULL DEFAULT 'PUBLIC',
  avatar_key                   TEXT,
  require_message_on_rerequest BOOLEAN NOT NULL DEFAULT TRUE,
  created_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_channel_owner ON channel(owner_id);

CREATE TABLE channel_file (
  channel_id     UUID NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
  file_id        UUID NOT NULL REFERENCES file(id) ON DELETE CASCADE,
  display_label  TEXT NOT NULL,
  added_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (channel_id, file_id)
);
CREATE INDEX ix_channel_file_file ON channel_file(file_id);

CREATE TABLE membership (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  channel_id       UUID NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
  user_id          UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  status           membership_status NOT NULL DEFAULT 'PENDING',
  request_message  TEXT,
  approved_at      TIMESTAMPTZ,
  rejected_at      TIMESTAMPTZ,
  banned_at        TIMESTAMPTZ,
  left_at          TIMESTAMPTZ,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (channel_id, user_id)
);
CREATE INDEX ix_membership_channel_status ON membership(channel_id, status);
CREATE INDEX ix_membership_user ON membership(user_id, status);
