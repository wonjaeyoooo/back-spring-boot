CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    supabase_user_id UUID NOT NULL UNIQUE,
    email            VARCHAR(255) NOT NULL UNIQUE,
    nickname         VARCHAR(50),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
