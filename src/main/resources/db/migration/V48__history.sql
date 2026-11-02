-- The sheet's history, for a statement taken as of a past date: each change is kept with the sheet's
-- today when it was made (on_date), in the order made (id). The state as of the end of a date is what
-- the changes made while the sheet's today was on or before it left. What was there before this
-- history was kept counts from the beginning of time (0001-01-01).

-- A person counts from the sheet's today when they were added.
ALTER TABLE person ADD COLUMN added_on DATE DEFAULT DATE '0001-01-01' NOT NULL;

-- A hold counts from the sheet's today when it was given.
ALTER TABLE person_hold ADD COLUMN held_on DATE DEFAULT DATE '0001-01-01' NOT NULL;

-- A claim's values after each change to it (the ones its figures and statement binding depend on).
CREATE TABLE claim_history (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id       VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    on_date        DATE           NOT NULL,
    claim_id       BIGINT         NOT NULL,
    person_id      BIGINT         NOT NULL,
    amount         DECIMAL(14, 2) NOT NULL,
    claim_date     DATE           NOT NULL,
    status         VARCHAR(10)    NOT NULL,
    settled_due    DECIMAL(19, 2),
    stated         BOOLEAN        NOT NULL,
    stated_due     DECIMAL(19, 2),
    stated_na      BOOLEAN        NOT NULL,
    corrects       VARCHAR(600),
    due_by         DATE           NOT NULL,
    deferred_until DATE
);
CREATE INDEX claim_history_sheet ON claim_history (sheet_id, claim_id, id);
INSERT INTO claim_history (sheet_id, on_date, claim_id, person_id, amount, claim_date, status, settled_due, stated,
        stated_due, stated_na, corrects, due_by, deferred_until)
    SELECT sheet_id, DATE '0001-01-01', id, person_id, amount, claim_date, status, settled_due, stated, stated_due,
        stated_na, corrects, due_by, deferred_until FROM claim ORDER BY id;

-- Each rate set: it replaces the grade's rate from the same from-date.
CREATE TABLE rate_history (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    on_date   DATE          NOT NULL,
    grade     VARCHAR(1)    NOT NULL,
    pct       DECIMAL(7, 2) NOT NULL,
    from_date DATE,
    to_date   DATE
);
CREATE INDEX rate_history_sheet ON rate_history (sheet_id, id);
INSERT INTO rate_history (sheet_id, on_date, grade, pct, from_date, to_date)
    SELECT sheet_id, DATE '0001-01-01', grade, pct, from_date, to_date FROM rate;

-- Each grade given to a person: it replaces the one they had from the same from-date.
CREATE TABLE grade_history (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    on_date   DATE         NOT NULL,
    person_id BIGINT       NOT NULL,
    grade     VARCHAR(1)   NOT NULL,
    from_date DATE
);
CREATE INDEX grade_history_sheet ON grade_history (sheet_id, person_id, id);
INSERT INTO grade_history (sheet_id, on_date, person_id, grade, from_date)
    SELECT sheet_id, DATE '0001-01-01', person_id, grade, from_date FROM person_grade;

-- Each dated setting value recorded: it replaces the one recorded from the same from-date.
CREATE TABLE setting_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200) NOT NULL REFERENCES sheet (id),
    on_date     DATE         NOT NULL,
    setting_key VARCHAR(20)  NOT NULL,
    from_date   DATE         NOT NULL,
    setting     VARCHAR(20)
);
CREATE INDEX setting_history_sheet ON setting_history (sheet_id, id);
INSERT INTO setting_history (sheet_id, on_date, setting_key, from_date, setting)
    SELECT sheet_id, DATE '0001-01-01', setting_key, from_date, setting FROM setting_value;

-- Each policy date set (null: blank, the sheet's today).
CREATE TABLE policy_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200) NOT NULL REFERENCES sheet (id),
    on_date     DATE         NOT NULL,
    policy_date DATE
);
INSERT INTO policy_history (sheet_id, on_date, policy_date)
    SELECT id, DATE '0001-01-01', policy_date FROM sheet WHERE policy_date IS NOT NULL;

-- Each retirement of a person.
CREATE TABLE retire_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(200) NOT NULL REFERENCES sheet (id),
    on_date      DATE         NOT NULL,
    person_id    BIGINT       NOT NULL,
    retired_from DATE         NOT NULL
);
INSERT INTO retire_history (sheet_id, on_date, person_id, retired_from)
    SELECT sheet_id, DATE '0001-01-01', id, retired_from FROM person WHERE retired_from IS NOT NULL;
