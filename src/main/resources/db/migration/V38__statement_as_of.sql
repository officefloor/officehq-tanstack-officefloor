-- A statement can be taken as of a date on or before the sheet's today. taken_on is then that date (the statement
-- date), as_of is it too (null: taken on the sheet's today), and made_on is the sheet's today when it was taken.
ALTER TABLE statement ADD COLUMN as_of DATE;
ALTER TABLE statement ADD COLUMN made_on DATE;
UPDATE statement SET made_on = taken_on;

-- To read the sheet back as it stood at the end of a date, each change keeps the sheet's today when it was made
-- (made_on). Rows stored before this keep 0001-01-01: made before every date.

-- The sheet's today when the person was added (null: added before this was kept; there from the beginning).
ALTER TABLE person ADD COLUMN added_on DATE;

ALTER TABLE person_hold ADD COLUMN made_on DATE DEFAULT DATE '0001-01-01' NOT NULL;

-- Every retirement date given a person, in the order given (the last one made on or before a date is in force then).
CREATE TABLE person_retire_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200) NOT NULL,
    person_id  BIGINT       NOT NULL REFERENCES person (id),
    from_date  DATE         NOT NULL,
    made_on    DATE         NOT NULL
);
CREATE INDEX person_retire_log_sheet ON person_retire_log (sheet_id, person_id, id);
INSERT INTO person_retire_log (sheet_id, person_id, from_date, made_on)
    SELECT sheet_id, id, retired_from, DATE '0001-01-01' FROM person WHERE retired_from IS NOT NULL;

-- Every list of dated grades stored for a person, in the order stored: grades is "grade@from" (from blank for none)
-- joined by ";".
CREATE TABLE person_grade_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)  NOT NULL,
    person_id  BIGINT        NOT NULL REFERENCES person (id),
    grades     VARCHAR(2000) NOT NULL,
    made_on    DATE          NOT NULL
);
CREATE INDEX person_grade_log_sheet ON person_grade_log (sheet_id, person_id, id);

-- Every rate stored, in the order stored (a later one replaces an earlier one of the same grade and from-date).
CREATE TABLE rate_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)  NOT NULL,
    grade      VARCHAR(1)    NOT NULL,
    rate       DECIMAL(7, 2) NOT NULL,
    from_date  DATE,
    to_date    DATE,
    made_on    DATE          NOT NULL
);
CREATE INDEX rate_log_sheet ON rate_log (sheet_id, id);
INSERT INTO rate_log (sheet_id, grade, rate, from_date, to_date, made_on)
    SELECT sheet_id, grade, rate, from_date, to_date, DATE '0001-01-01' FROM rate;

-- Every setting value stored, in the order stored (a later one replaces an earlier one of the same key and from-date).
CREATE TABLE setting_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id      VARCHAR(200) NOT NULL,
    setting_key   VARCHAR(50)  NOT NULL,
    setting_value VARCHAR(2000),
    from_date     DATE         NOT NULL,
    made_on       DATE         NOT NULL
);
CREATE INDEX setting_log_sheet ON setting_log (sheet_id, id);
INSERT INTO setting_log (sheet_id, setting_key, setting_value, from_date, made_on)
    SELECT sheet_id, setting_key, setting_value, from_date, DATE '0001-01-01' FROM setting;
