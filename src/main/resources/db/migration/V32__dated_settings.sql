-- Dated settings: each figure-carrying setting (clerk-limit, floor, terms, terms-<grade>, late-pct, cap)
-- is recorded with the date it is in force from. The value in force on a date is the one with the latest
-- from-date on or before it; before every from-date the sheet's starting value (floor 1.00, late-pct 0.00,
-- terms 14; none for the others). A NULL value: none (no clerk limit, no cap) from that date.
CREATE TABLE sheet_setting (
    sheet_id      VARCHAR(200)  NOT NULL,
    setting_key   VARCHAR(40)   NOT NULL,
    from_date     DATE          NOT NULL,
    setting_value DECIMAL(12,2),
    PRIMARY KEY (sheet_id, setting_key, from_date)
);

-- The values already set carry over, in force from the start.
INSERT INTO sheet_setting SELECT id, 'clerk-limit', DATE '0001-01-01', clerk_limit FROM sheet WHERE clerk_limit IS NOT NULL;
INSERT INTO sheet_setting SELECT id, 'floor', DATE '0001-01-01', due_floor FROM sheet WHERE due_floor <> 1.00;
INSERT INTO sheet_setting SELECT id, 'terms', DATE '0001-01-01', terms_days FROM sheet WHERE terms_days <> 14;
INSERT INTO sheet_setting SELECT id, 'late-pct', DATE '0001-01-01', late_pct FROM sheet WHERE late_pct <> 0.00;
INSERT INTO sheet_setting SELECT id, 'cap', DATE '0001-01-01', due_cap FROM sheet WHERE due_cap IS NOT NULL;
INSERT INTO sheet_setting SELECT sheet_id, 'terms-' || TRIM(grade), DATE '0001-01-01', terms_days FROM sheet_grade_terms;

ALTER TABLE sheet DROP COLUMN clerk_limit;
ALTER TABLE sheet DROP COLUMN due_floor;
ALTER TABLE sheet DROP COLUMN terms_days;
ALTER TABLE sheet DROP COLUMN late_pct;
ALTER TABLE sheet DROP COLUMN due_cap;
DROP TABLE sheet_grade_terms;
