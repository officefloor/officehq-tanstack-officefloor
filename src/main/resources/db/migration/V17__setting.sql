-- The sheet's settings, one row per sheet and key; a key with no row takes its default. The clerk-limit
-- (two decimals) caps the due a clerk may settle; with no row (or a blank one) there is no limit.
CREATE TABLE setting (
    sheet         VARCHAR(200) NOT NULL,
    setting_key   VARCHAR(100) NOT NULL,
    setting_value VARCHAR(200) NOT NULL,
    PRIMARY KEY (sheet, setting_key)
);
