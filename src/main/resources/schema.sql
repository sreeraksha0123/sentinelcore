-- =============================================================
-- SentinelCore — Database Schema
-- PostgreSQL 15+
-- JPA ddl-auto=update will also manage this, but this file
-- is kept as reference and for explicit control.
-- =============================================================

-- ── Users ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP            DEFAULT now()
);

-- ── Subscriptions (one-to-one with users) ────────────────────
CREATE TABLE IF NOT EXISTS subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT UNIQUE NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan       VARCHAR(50)  NOT NULL DEFAULT 'FREE',
    status     VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP            DEFAULT now(),
    updated_at TIMESTAMP            DEFAULT now()
);

-- ── Entitlements (seeded per plan) ───────────────────────────
CREATE TABLE IF NOT EXISTS entitlements (
    id      BIGSERIAL PRIMARY KEY,
    plan    VARCHAR(50)  NOT NULL,
    feature VARCHAR(100) NOT NULL,
    enabled BOOLEAN      NOT NULL DEFAULT true,
    UNIQUE (plan, feature)
);

-- ── Indexes ──────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_entitlements_plan     ON entitlements (plan);
