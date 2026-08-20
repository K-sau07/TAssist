-- V2: users
CREATE TABLE app_user (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email           CITEXT UNIQUE NOT NULL,
  display_name    TEXT NOT NULL,
  password_hash   TEXT,
  auth_provider   auth_provider NOT NULL,
  google_subject  TEXT UNIQUE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (
    (auth_provider = 'PASSWORD' AND password_hash IS NOT NULL AND google_subject IS NULL)
    OR
    (auth_provider = 'GOOGLE' AND google_subject IS NOT NULL AND password_hash IS NULL)
  )
);
