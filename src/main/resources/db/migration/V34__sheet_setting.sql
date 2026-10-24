-- A sheet's dated settings: the value of a setting that carries a figure (floor, late-pct, terms, terms-A/B/C,
-- clerk-limit, cap) from a from-date on. The value in force on a date is the one with the latest from-date on or
-- before it; a row with no from-date applies from the beginning of time; before every from-date the sheet's
-- starting value applies (floor 1.00, late-pct 0.00, terms 14; none for the others). The value is as the screen
-- shows it; null (blank) for none. The sheet's old setting columns are no longer read: their values are carried
-- over here, from the beginning of time.
CREATE TABLE sheet_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    setting_key VARCHAR(40) NOT NULL,
    from_date DATE,
    setting_value VARCHAR(40)
);
CREATE INDEX sheet_setting_sheet ON sheet_setting (sheet_id, setting_key);
INSERT INTO sheet_setting (sheet_id, setting_key, setting_value)
    SELECT id, 'floor', CAST(floor AS VARCHAR) FROM sheet WHERE floor <> 1.00;
INSERT INTO sheet_setting (sheet_id, setting_key, setting_value)
    SELECT id, 'late-pct', CAST(late_pct AS VARCHAR) FROM sheet WHERE late_pct <> 0.00;
INSERT INTO sheet_setting (sheet_id, setting_key, setting_value)
    SELECT id, 'terms', CAST(terms AS VARCHAR) FROM sheet WHERE terms <> 14;
INSERT INTO sheet_setting (sheet_id, setting_key, setting_value)
    SELECT id, 'terms-A', CAST(terms_a AS VARCHAR) FROM sheet WHERE terms_a IS NOT NULL;
INSERT INTO sheet_setting (sheet_id, setting_key, setting_value)
    SELECT id, 'terms-B', CAST(terms_b AS VARCHAR) FROM sheet WHERE terms_b IS NOT NULL;
INSERT INTO sheet_setting (sheet_id, setting_key, setting_value)
    SELECT id, 'terms-C', CAST(terms_c AS VARCHAR) FROM sheet WHERE terms_c IS NOT NULL;
INSERT INTO sheet_setting (sheet_id, setting_key, setting_value)
    SELECT id, 'clerk-limit', CAST(clerk_limit AS VARCHAR) FROM sheet WHERE clerk_limit IS NOT NULL;
INSERT INTO sheet_setting (sheet_id, setting_key, setting_value)
    SELECT id, 'cap', CAST(cap AS VARCHAR) FROM sheet WHERE cap IS NOT NULL;
