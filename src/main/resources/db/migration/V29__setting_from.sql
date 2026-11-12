-- The settings that carry a figure are dated: each value is recorded from a from-date, and the value in
-- force on a date is the one with the latest from-date on or before it (before every from-date, the
-- sheet's starting value). A null value is blank (for clerk-limit and cap: none; for a grade's terms:
-- the sheet's). The policy date and the period numbering date stay undated in sheet_setting.
CREATE TABLE sheet_setting_from (
    sheet_id      VARCHAR(255) NOT NULL,
    setting_key   VARCHAR(64)  NOT NULL,
    from_date     DATE         NOT NULL,
    setting_value VARCHAR(255),
    PRIMARY KEY (sheet_id, setting_key, from_date)
);
-- A value set before settings were dated holds from the start.
INSERT INTO sheet_setting_from (sheet_id, setting_key, from_date, setting_value)
    SELECT sheet_id, setting_key, DATE '0001-01-01', setting_value FROM sheet_setting
    WHERE setting_key NOT IN ('policy-date', 'period-numbering-from');
DELETE FROM sheet_setting WHERE setting_key NOT IN ('policy-date', 'period-numbering-from');
