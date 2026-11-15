-- What the sheet held as of a date, so a statement can be taken as of it: each change is kept with
-- the sheet's today when it was made (on_date). A change made while the sheet's today was on or before
-- a date counts as of it; a later one does not. Rows kept before this history have no on_date: they
-- count as of every date.

-- The sheet's today when the person was added (none: added before it was kept).
ALTER TABLE person ADD COLUMN added_on DATE;
-- The sheet's today when the hold was added (none: added before it was kept).
ALTER TABLE person_hold ADD COLUMN added_on DATE;

-- Each claim as it stood after every change to it, oldest first.
CREATE TABLE claim_history (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id       VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    claim_id       BIGINT         NOT NULL REFERENCES claim (id),
    on_date        DATE,
    person_id      BIGINT         NOT NULL,
    amount         DECIMAL(19, 2) NOT NULL,
    claim_date     DATE           NOT NULL,
    status         VARCHAR(10)    NOT NULL,
    settled_due    DECIMAL(19, 2),
    stated         BOOLEAN        NOT NULL,
    stated_due     DECIMAL(19, 2),
    deferred_until DATE,
    due_by         DATE           NOT NULL
);
CREATE INDEX claim_history_claim ON claim_history (sheet_id, claim_id, id);
INSERT INTO claim_history (sheet_id, claim_id, on_date, person_id, amount, claim_date, status, settled_due,
        stated, stated_due, deferred_until, due_by)
    SELECT sheet_id, id, NULL, person_id, amount, claim_date, status, settled_due, stated, stated_due,
        deferred_until, due_by FROM claim;

-- Each rate set, oldest first (a later one with the same grade and from-date replaces it).
CREATE TABLE rate_history (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)   NOT NULL,
    grade     VARCHAR(10)    NOT NULL,
    pct       DECIMAL(19, 2) NOT NULL,
    from_date DATE,
    to_date   DATE,
    from_key  DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01')),
    on_date   DATE
);
CREATE INDEX rate_history_sheet ON rate_history (sheet_id, grade, id);
INSERT INTO rate_history (sheet_id, grade, pct, from_date, to_date, on_date)
    SELECT sheet_id, grade, pct, from_date, to_date, NULL FROM rate;

-- Each setting value recorded, oldest first (a later one with the same key and from-date replaces it).
CREATE TABLE setting_history (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    setting   VARCHAR(200) NOT NULL,
    val       VARCHAR(200),
    from_date DATE,
    from_key  DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01')),
    on_date   DATE
);
CREATE INDEX setting_history_sheet ON setting_history (sheet_id, setting, id);
INSERT INTO setting_history (sheet_id, setting, val, from_date, on_date)
    SELECT sheet_id, setting, val, from_date, NULL FROM sheet_setting;

-- Each grade a person was given, oldest first (a later one with the same from-date replaces it).
CREATE TABLE person_grade_history (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    grade     VARCHAR(10)  NOT NULL,
    from_date DATE,
    from_key  DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01')),
    on_date   DATE
);
CREATE INDEX person_grade_history_person ON person_grade_history (sheet_id, person_id, id);
INSERT INTO person_grade_history (sheet_id, person_id, grade, from_date, on_date)
    SELECT sheet_id, person_id, grade, from_date, NULL FROM person_grade;

-- Each retirement date a person was given, oldest first (the last one stands).
CREATE TABLE person_retired_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(200) NOT NULL,
    person_id    BIGINT       NOT NULL,
    retired_from DATE         NOT NULL,
    on_date      DATE
);
CREATE INDEX person_retired_history_person ON person_retired_history (sheet_id, person_id, id);
INSERT INTO person_retired_history (sheet_id, person_id, retired_from, on_date)
    SELECT sheet_id, id, retired_from, NULL FROM person WHERE retired_from IS NOT NULL;
