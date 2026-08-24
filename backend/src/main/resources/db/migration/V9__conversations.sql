-- V9: channel human messaging (DM + group) — 02_MESSAGING_SPEC.md
-- Additive only: new enums, new tables, one safe column on channel. Existing tables untouched.

-- ── enums ──
CREATE TYPE conversation_kind AS ENUM ('DM','GROUP');
CREATE TYPE msg_sender_kind   AS ENUM ('HUMAN','AI');

-- ── conversation ──
-- DMs hold two participants in canonical order (participant_a < participant_b) so a pair
-- maps to exactly one row regardless of who opens it. GROUP has neither participant.
CREATE TABLE conversation (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  channel_id     UUID NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
  kind           conversation_kind NOT NULL,
  participant_a  UUID REFERENCES app_user(id) ON DELETE CASCADE,
  participant_b  UUID REFERENCES app_user(id) ON DELETE CASCADE,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (
    (kind = 'DM'    AND participant_a IS NOT NULL AND participant_b IS NOT NULL AND participant_a <> participant_b)
    OR (kind = 'GROUP' AND participant_a IS NULL AND participant_b IS NULL)
  )
);
-- one DM per (channel, ordered participant pair); one GROUP per channel
CREATE UNIQUE INDEX ux_conversation_dm    ON conversation(channel_id, participant_a, participant_b) WHERE kind = 'DM';
CREATE UNIQUE INDEX ux_conversation_group ON conversation(channel_id) WHERE kind = 'GROUP';
CREATE INDEX ix_conversation_channel      ON conversation(channel_id, updated_at DESC);
CREATE INDEX ix_conversation_participant_a ON conversation(participant_a) WHERE participant_a IS NOT NULL;
CREATE INDEX ix_conversation_participant_b ON conversation(participant_b) WHERE participant_b IS NOT NULL;

-- ── conversation_message ──
CREATE TABLE conversation_message (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id  UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
  sender_kind      msg_sender_kind NOT NULL,
  sender_id        UUID REFERENCES app_user(id) ON DELETE SET NULL,
  content          TEXT NOT NULL,
  citations        JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at       TIMESTAMPTZ,
  CHECK (
    (sender_kind = 'HUMAN' AND sender_id IS NOT NULL)
    OR (sender_kind = 'AI')
  )
);
CREATE INDEX ix_convmsg_conversation ON conversation_message(conversation_id, created_at ASC);

-- ── conversation_read (read receipts + unread badges) ──
CREATE TABLE conversation_read (
  conversation_id  UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
  user_id          UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  last_read_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (conversation_id, user_id)
);

-- ── channel: owner-toggleable group room (safe default on) ──
ALTER TABLE channel ADD COLUMN group_chat_enabled BOOLEAN NOT NULL DEFAULT TRUE;
