-- A setting that carries a figure (the clerk limit, the floor, the terms and each grade's terms, the late percent,
-- the cap) is dated: one value per from-date, in force from that date until the next one. A blank value is none
-- (no clerk limit, no cap). The policy date and the period numbering from-date stay undated, in setting.
CREATE TABLE setting_from (
    sheet_id      VARCHAR(255) NOT NULL,
    setting_key   VARCHAR(64)  NOT NULL,
    from_date     DATE         NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (sheet_id, setting_key, from_date)
);
-- A value saved before settings were dated was in force on every date.
INSERT INTO setting_from (sheet_id, setting_key, from_date, setting_value)
    SELECT sheet_id, setting_key, DATE '0001-01-01', setting_value FROM setting
    WHERE setting_key IN ('clerk-limit', 'floor', 'terms', 'terms-A', 'terms-B', 'terms-C', 'late-pct', 'cap');
DELETE FROM setting
    WHERE setting_key IN ('clerk-limit', 'floor', 'terms', 'terms-A', 'terms-B', 'terms-C', 'late-pct', 'cap');
