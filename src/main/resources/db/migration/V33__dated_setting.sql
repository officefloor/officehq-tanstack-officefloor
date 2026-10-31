-- Settings that carry a figure are dated: each value is recorded from a from-date, and the value in
-- force on a date is the one with the latest from-date on or before it. Keys: clerk-limit, floor,
-- terms, terms-<grade>, late-pct and cap. A null value is a blank one: no clerk limit, no cap, or a
-- grade's terms that use the sheet's. Before every from-date a key has its starting value (floor
-- 1.00, late-pct 0.00, terms 14; none for the others). Saving a from-date already recorded replaces it.
CREATE TABLE sheet_setting_dated (
    sheet_id      VARCHAR(200)  NOT NULL,
    setting_key   VARCHAR(20)   NOT NULL,
    from_date     DATE          NOT NULL,
    setting_value DECIMAL(12,2),
    PRIMARY KEY (sheet_id, setting_key, from_date)
);
-- Values set before settings were dated were in force on every date: they are kept from the earliest date.
INSERT INTO sheet_setting_dated (sheet_id, setting_key, from_date, setting_value)
    SELECT sheet_id, 'clerk-limit', DATE '0001-01-01', clerk_limit FROM sheet_setting WHERE clerk_limit IS NOT NULL;
INSERT INTO sheet_setting_dated (sheet_id, setting_key, from_date, setting_value)
    SELECT sheet_id, 'floor', DATE '0001-01-01', floor FROM sheet_setting WHERE floor IS NOT NULL;
INSERT INTO sheet_setting_dated (sheet_id, setting_key, from_date, setting_value)
    SELECT sheet_id, 'terms', DATE '0001-01-01', terms FROM sheet_setting WHERE terms IS NOT NULL;
INSERT INTO sheet_setting_dated (sheet_id, setting_key, from_date, setting_value)
    SELECT sheet_id, 'late-pct', DATE '0001-01-01', late_pct FROM sheet_setting WHERE late_pct IS NOT NULL;
INSERT INTO sheet_setting_dated (sheet_id, setting_key, from_date, setting_value)
    SELECT sheet_id, 'cap', DATE '0001-01-01', cap FROM sheet_setting WHERE cap IS NOT NULL;
INSERT INTO sheet_setting_dated (sheet_id, setting_key, from_date, setting_value)
    SELECT sheet_id, 'terms-' || grade, DATE '0001-01-01', terms FROM sheet_grade_terms;
