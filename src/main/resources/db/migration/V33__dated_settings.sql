-- The sheet's settings that carry a figure are dated: each key (floor, late-pct, terms, terms-A/B/C, clerk-limit,
-- cap) holds at most one value per from-date. The value in force on a date is the one with the latest from-date on
-- or before it; before every from-date the sheet's starting value applies (floor 1.00, late-pct 0.00, terms 14,
-- a grade's terms: the sheet's terms, clerk-limit and cap: none). A NULL value is blank: none (clerk-limit, cap) or
-- the sheet's terms (a grade's terms). The policy date and the period numbering from stay undated, on the sheet.
CREATE TABLE setting_value (
    sheet_id    VARCHAR(100) NOT NULL REFERENCES sheet(id),
    setting_key VARCHAR(20)  NOT NULL,
    from_date   DATE         NOT NULL,
    val         VARCHAR(20),
    PRIMARY KEY (sheet_id, setting_key, from_date)
);
-- Values already kept carry over in force from the beginning of time.
INSERT INTO setting_value (sheet_id, setting_key, from_date, val)
    SELECT id, 'floor', DATE '0001-01-01', CAST(due_floor AS VARCHAR(20)) FROM sheet WHERE due_floor <> 1.00;
INSERT INTO setting_value (sheet_id, setting_key, from_date, val)
    SELECT id, 'late-pct', DATE '0001-01-01', CAST(late_pct AS VARCHAR(20)) FROM sheet WHERE late_pct <> 0.00;
INSERT INTO setting_value (sheet_id, setting_key, from_date, val)
    SELECT id, 'terms', DATE '0001-01-01', CAST(terms_days AS VARCHAR(20)) FROM sheet WHERE terms_days <> 14;
INSERT INTO setting_value (sheet_id, setting_key, from_date, val)
    SELECT id, 'clerk-limit', DATE '0001-01-01', CAST(clerk_limit AS VARCHAR(20)) FROM sheet WHERE clerk_limit IS NOT NULL;
INSERT INTO setting_value (sheet_id, setting_key, from_date, val)
    SELECT id, 'cap', DATE '0001-01-01', CAST(due_cap AS VARCHAR(20)) FROM sheet WHERE due_cap IS NOT NULL;
INSERT INTO setting_value (sheet_id, setting_key, from_date, val)
    SELECT sheet_id, 'terms-' || grade, DATE '0001-01-01', CAST(terms_days AS VARCHAR(20)) FROM grade_terms;
DROP TABLE grade_terms;
ALTER TABLE sheet DROP COLUMN due_floor;
ALTER TABLE sheet DROP COLUMN late_pct;
ALTER TABLE sheet DROP COLUMN terms_days;
ALTER TABLE sheet DROP COLUMN clerk_limit;
ALTER TABLE sheet DROP COLUMN due_cap;
