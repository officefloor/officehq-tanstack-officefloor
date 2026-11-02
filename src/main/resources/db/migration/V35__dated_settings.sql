-- A sheet's dated settings: each value a setting that carries a figure (clerk-limit, floor, terms,
-- terms-<grade>, late-pct, cap) has from a from-date. The value in force on a date is the one with the
-- latest from-date on or before it; before every from-date the setting has its starting value (floor
-- 1.00, late-pct 0.00, terms 14; none for the others). A null value is a blank one (no limit, no cap, a
-- grade using the sheet's terms).
CREATE TABLE setting_value (
    sheet_id    VARCHAR(200) NOT NULL REFERENCES sheet (id),
    setting_key VARCHAR(20)  NOT NULL,
    from_date   DATE         NOT NULL,
    setting     VARCHAR(20),
    PRIMARY KEY (sheet_id, setting_key, from_date)
);

-- The settings already set hold from the beginning of time.
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting)
    SELECT id, 'clerk-limit', DATE '0001-01-01', CAST(clerk_limit AS VARCHAR(20)) FROM sheet WHERE clerk_limit IS NOT NULL;
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting)
    SELECT id, 'floor', DATE '0001-01-01', CAST(floor AS VARCHAR(20)) FROM sheet WHERE floor <> 1.00;
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting)
    SELECT id, 'terms', DATE '0001-01-01', CAST(terms AS VARCHAR(20)) FROM sheet WHERE terms <> 14;
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting)
    SELECT id, 'late-pct', DATE '0001-01-01', CAST(late_pct AS VARCHAR(20)) FROM sheet WHERE late_pct <> 0.00;
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting)
    SELECT id, 'cap', DATE '0001-01-01', CAST(cap AS VARCHAR(20)) FROM sheet WHERE cap IS NOT NULL;
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting)
    SELECT sheet_id, 'terms-' || grade, DATE '0001-01-01', CAST(terms AS VARCHAR(20)) FROM grade_terms;

ALTER TABLE sheet DROP COLUMN clerk_limit;
ALTER TABLE sheet DROP COLUMN floor;
ALTER TABLE sheet DROP COLUMN terms;
ALTER TABLE sheet DROP COLUMN late_pct;
ALTER TABLE sheet DROP COLUMN cap;
DROP TABLE grade_terms;
