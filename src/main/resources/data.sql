-- =============================================================
-- SentinelCore — Seed Data
-- Entitlements per subscription tier
-- =============================================================

INSERT INTO entitlements (plan, feature, enabled)
VALUES
  -- FREE tier
  ('FREE', 'api_calls_per_day',  true),
  ('FREE', 'export_csv',         false),
  ('FREE', 'custom_sso',         false),
  ('FREE', 'priority_support',   false),
  ('FREE', 'audit_logs',         false),

  -- PRO tier
  ('PRO',  'api_calls_per_day',  true),
  ('PRO',  'export_csv',         true),
  ('PRO',  'custom_sso',         false),
  ('PRO',  'priority_support',   true),
  ('PRO',  'audit_logs',         false),

  -- ENTERPRISE tier
  ('ENTERPRISE', 'api_calls_per_day', true),
  ('ENTERPRISE', 'export_csv',        true),
  ('ENTERPRISE', 'custom_sso',        true),
  ('ENTERPRISE', 'priority_support',  true),
  ('ENTERPRISE', 'audit_logs',        true)

ON CONFLICT (plan, feature) DO NOTHING;
