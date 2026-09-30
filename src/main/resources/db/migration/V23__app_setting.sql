-- A tiny key/value store for whole-app settings that are not tied to any domain entity. The first
-- use is the dashboard's "as of" reference date: the fixed day overdue is measured against, seeded
-- per spec so the overdue count is deterministic. Key is the primary key; value is an ISO date
-- literal (or any short string). Additive and standalone — no FK, no existing table touched.
CREATE TABLE app_setting (
    setting_key   VARCHAR(64) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL
);
