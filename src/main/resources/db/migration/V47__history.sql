-- What the sheet held as of a date, for a statement taken as of it: every change that bears on a person's figures is
-- kept with the sheet's today when it was made (made_on; NULL: before history was kept, so it counts on every date).
-- A change counts as of a date when it was made while the sheet's today was on or before that date.

-- The day a person was added (NULL: before history was kept).
ALTER TABLE person ADD COLUMN added_on DATE;

-- The day each hold was put on.
ALTER TABLE person_hold ADD COLUMN made_on DATE;

-- Each retirement date a person was given, in the order given.
CREATE TABLE person_retire_log (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    person_id BIGINT       NOT NULL REFERENCES person(id),
    from_date DATE,
    made_on   DATE
);
CREATE INDEX person_retire_log_sheet ON person_retire_log(sheet_id, person_id, id);
INSERT INTO person_retire_log (sheet_id, person_id, from_date, made_on)
    SELECT sheet_id, id, retired_from, NULL FROM person WHERE retired_from IS NOT NULL;

-- Each grade a person was given, in the order given (one from the same from-date replaces the one before it).
CREATE TABLE person_grade_log (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    person_id BIGINT       NOT NULL REFERENCES person(id),
    grade     CHAR(1)      NOT NULL,
    from_date DATE,
    made_on   DATE
);
CREATE INDEX person_grade_log_sheet ON person_grade_log(sheet_id, person_id, id);
INSERT INTO person_grade_log (sheet_id, person_id, grade, from_date, made_on)
    SELECT sheet_id, person_id, grade, from_date, NULL FROM person_grade;

-- Each rate set, in the order set (one from the same from-date replaces the one before it).
CREATE TABLE rate_log (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100)  NOT NULL REFERENCES sheet(id),
    grade     CHAR(1)       NOT NULL,
    pct       DECIMAL(5, 2) NOT NULL,
    from_date DATE,
    to_date   DATE,
    made_on   DATE
);
CREATE INDEX rate_log_sheet ON rate_log(sheet_id, id);
INSERT INTO rate_log (sheet_id, grade, pct, from_date, to_date, made_on)
    SELECT sheet_id, grade, pct, from_date, to_date, NULL FROM rate;

-- Each dated setting value recorded, in the order recorded (one from the same from-date replaces the one before it).
CREATE TABLE setting_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(100) NOT NULL REFERENCES sheet(id),
    setting_key VARCHAR(20)  NOT NULL,
    from_date   DATE         NOT NULL,
    val         VARCHAR(20),
    made_on     DATE
);
CREATE INDEX setting_log_sheet ON setting_log(sheet_id, id);
INSERT INTO setting_log (sheet_id, setting_key, from_date, val, made_on)
    SELECT sheet_id, setting_key, from_date, val, NULL FROM setting_value;

-- Each claim as it stood after every change to it, oldest first: a claim with none on or before a date was not
-- yet added then.
CREATE TABLE claim_history (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id       VARCHAR(100)   NOT NULL REFERENCES sheet(id),
    claim_id       BIGINT         NOT NULL REFERENCES claim(id),
    person_id      BIGINT         NOT NULL,
    amount         DECIMAL(14, 2) NOT NULL,
    claim_date     DATE           NOT NULL,
    note           VARCHAR(1000)  NOT NULL,
    status         VARCHAR(10)    NOT NULL,
    claim_number   VARCHAR(220),
    settled_due    DECIMAL(14, 2),
    opened_by      VARCHAR(100),
    stated         BOOLEAN        NOT NULL,
    stated_due     DECIMAL(14, 2),
    corrects       VARCHAR(220),
    due_by         DATE           NOT NULL,
    deferred_until DATE,
    proofs         INT            NOT NULL,
    split_from     VARCHAR(220),
    late_entry     BOOLEAN        NOT NULL,
    made_on        DATE
);
CREATE INDEX claim_history_sheet ON claim_history(sheet_id, claim_id, id);
INSERT INTO claim_history (sheet_id, claim_id, person_id, amount, claim_date, note, status, claim_number, settled_due,
        opened_by, stated, stated_due, corrects, due_by, deferred_until, proofs, split_from, late_entry, made_on)
    SELECT sheet_id, id, person_id, amount, claim_date, note, status, claim_number, settled_due, opened_by, stated,
        stated_due, corrects, due_by, deferred_until, proofs, split_from, late_entry, NULL FROM claim;
