-- A small key/value store for app-wide settings the dashboard reads. The first setting is the
-- "as of" reference date the dashboard measures overdue invoices against, so a seeded fixture makes
-- the overdue count deterministic; in a real deploy it is absent and the dashboard uses today.
-- Additive: existing tables are untouched, and a new setting is one more row.
CREATE TABLE app_settings (
    setting_key   VARCHAR(64)  PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL
);
