-- History kept so a statement can be taken as of a past date: every change that moves a figure is recorded
-- with the sheet's today when it was made (set_on). The store as it stood at the end of a date is every change
-- made while the sheet's today was on or before it, the latest of each winning. A NULL set_on is a value carried
-- over from before this history was kept: it counts as of any date.

-- The sheet's today when the person was added; NULL when added before this was kept.
ALTER TABLE person ADD COLUMN added_on DATE;

-- Each claim's figure-moving columns after each change to them, in the order made.
CREATE TABLE claim_history (
    seq            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id       VARCHAR(200)  NOT NULL,
    claim_id       BIGINT        NOT NULL,
    set_on         DATE,
    person_id      BIGINT        NOT NULL,
    amount         DECIMAL(14,2) NOT NULL,
    claim_date     DATE          NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    settled_due    DECIMAL(14,2),
    stated         BOOLEAN       NOT NULL,
    stated_due     DECIMAL(14,2),
    due_by         DATE,
    deferred_until DATE,
    corrects       VARCHAR(100)
);
CREATE INDEX claim_history_sheet ON claim_history (sheet_id, claim_id, seq);
INSERT INTO claim_history (sheet_id, claim_id, set_on, person_id, amount, claim_date, status, settled_due, stated,
        stated_due, due_by, deferred_until, corrects)
    SELECT sheet_id, id, NULL, person_id, amount, claim_date, status, settled_due, stated, stated_due, due_by,
        deferred_until, corrects FROM claim;

-- Each rate set, in the order set (a later one from the same grade and from-date replaces it).
CREATE TABLE rate_history (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    set_on    DATE,
    grade     CHAR(1)      NOT NULL,
    rate      DECIMAL(6,2) NOT NULL,
    from_date DATE,
    to_date   DATE
);
INSERT INTO rate_history (sheet_id, set_on, grade, rate, from_date, to_date)
    SELECT sheet_id, NULL, grade, rate, from_date, to_date FROM rate;

-- Each dated setting recorded, in the order recorded (a later one from the same key and from-date replaces it).
CREATE TABLE setting_history (
    seq           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id      VARCHAR(200)  NOT NULL,
    set_on        DATE,
    setting_key   VARCHAR(40)   NOT NULL,
    from_date     DATE          NOT NULL,
    setting_value DECIMAL(12,2)
);
INSERT INTO setting_history (sheet_id, set_on, setting_key, from_date, setting_value)
    SELECT sheet_id, NULL, setting_key, from_date, setting_value FROM sheet_setting;

-- Each policy date set (NULL: blank), in the order set.
CREATE TABLE policy_history (
    seq         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200) NOT NULL,
    set_on      DATE,
    policy_date DATE
);
INSERT INTO policy_history (sheet_id, set_on, policy_date) SELECT id, NULL, policy_date FROM sheet WHERE policy_date IS NOT NULL;

-- Each grade recorded for a person, in the order recorded (a later one from the same from-date replaces it).
CREATE TABLE grade_history (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    set_on    DATE,
    from_date DATE,
    grade     CHAR(1)      NOT NULL
);
INSERT INTO grade_history (sheet_id, person_id, set_on, from_date, grade)
    SELECT sheet_id, person_id, NULL, from_date, grade FROM person_grade ORDER BY seq;

-- The sheet's today when each hold was added.
ALTER TABLE person_hold ADD COLUMN set_on DATE;

-- Each retirement date recorded for a person, in the order recorded.
CREATE TABLE retire_history (
    seq          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(200) NOT NULL,
    person_id    BIGINT       NOT NULL,
    set_on       DATE,
    retired_from DATE
);
INSERT INTO retire_history (sheet_id, person_id, set_on, retired_from)
    SELECT sheet_id, id, NULL, retired_from FROM person WHERE retired_from IS NOT NULL;
