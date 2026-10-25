-- The settings that carry a figure (the floor, the late percent, the terms and each grade's, the clerk limit, the cap)
-- are dated: each value is in force from its from-date until the next one's. A null value is blank. A value already
-- set before settings were dated is kept as in force from the beginning of time.
CREATE TABLE sheet_setting_from (
    sheet_id      VARCHAR(200) NOT NULL REFERENCES sheet (id),
    setting_key   VARCHAR(50)  NOT NULL,
    from_date     DATE         NOT NULL,
    setting_value VARCHAR(200),
    PRIMARY KEY (sheet_id, setting_key, from_date)
);

INSERT INTO sheet_setting_from (sheet_id, setting_key, from_date, setting_value)
SELECT sheet_id, setting_key, DATE '0001-01-01', setting_value FROM sheet_setting
WHERE setting_key NOT IN ('policy-date', 'period-numbering-from');

DELETE FROM sheet_setting WHERE setting_key NOT IN ('policy-date', 'period-numbering-from');
