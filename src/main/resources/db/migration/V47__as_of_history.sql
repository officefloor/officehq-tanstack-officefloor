-- The history a statement taken as of a date reads: every change below is kept with the sheet's
-- today when it was made (set_on / made_on / added_on), so the figures as they stood at the end of a
-- date are those of the changes made while the sheet's today was on or before it. What was already
-- stored before this history was kept counts from the beginning of time (0001-01-01, or null).

-- The sheet's today when the person was added (null: from the beginning of time).
ALTER TABLE person ADD COLUMN added_on DATE;

-- Every rate set, oldest first: the rate in force for a grade and from-date as of a date is the
-- last one set for them on or before it.
CREATE TABLE rate_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    grade CHAR(1) NOT NULL,
    pct DECIMAL(19, 2) NOT NULL,
    from_date DATE,
    to_date DATE,
    set_on DATE NOT NULL
);
CREATE INDEX rate_history_sheet ON rate_history(sheet_id, id);
INSERT INTO rate_history (sheet_id, grade, pct, from_date, to_date, set_on)
    SELECT sheet_id, grade, pct, from_date, to_date, DATE '0001-01-01' FROM rate;

-- Every dated setting value recorded, oldest first, and every policy date set (key policy-date,
-- no from-date, '' for none).
CREATE TABLE setting_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    setting_key VARCHAR(40) NOT NULL,
    from_date DATE,
    setting_value VARCHAR(40) NOT NULL,
    set_on DATE NOT NULL
);
CREATE INDEX setting_history_sheet ON setting_history(sheet_id, id);
INSERT INTO setting_history (sheet_id, setting_key, from_date, setting_value, set_on)
    SELECT sheet_id, setting_key, from_date, setting_value, DATE '0001-01-01' FROM setting_value;
INSERT INTO setting_history (sheet_id, setting_key, from_date, setting_value, set_on)
    SELECT id, 'policy-date', NULL, CAST(policy_date AS VARCHAR), DATE '0001-01-01' FROM sheet WHERE policy_date IS NOT NULL;

-- Every grade a person was given, oldest first: the first grade (no from-date) or a dated one.
CREATE TABLE person_grade_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    from_date DATE,
    grade CHAR(1) NOT NULL,
    set_on DATE NOT NULL
);
CREATE INDEX person_grade_history_sheet ON person_grade_history(sheet_id, person_id, id);
INSERT INTO person_grade_history (sheet_id, person_id, from_date, grade, set_on)
    SELECT sheet_id, id, NULL, grade, DATE '0001-01-01' FROM person;
INSERT INTO person_grade_history (sheet_id, person_id, from_date, grade, set_on)
    SELECT sheet_id, person_id, from_date, grade, DATE '0001-01-01' FROM person_grade;

-- Every retirement date a person was given, oldest first.
CREATE TABLE person_retirement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    from_date DATE,
    set_on DATE NOT NULL
);
CREATE INDEX person_retirement_sheet ON person_retirement(sheet_id, person_id, id);
INSERT INTO person_retirement (sheet_id, person_id, from_date, set_on)
    SELECT sheet_id, id, retired_from, DATE '0001-01-01' FROM person WHERE retired_from IS NOT NULL;

-- The sheet's today when a hold was added (null: from the beginning of time).
ALTER TABLE person_hold ADD COLUMN made_on DATE;

-- Every change to a claim's statement binding, oldest first: bound (with its figure, null: n/a) or
-- left unbound. A claim with no line on or before a date was unbound then.
CREATE TABLE claim_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    claim_id BIGINT NOT NULL REFERENCES claim(id),
    stated BOOLEAN NOT NULL,
    stated_due DECIMAL(19, 2),
    set_on DATE NOT NULL
);
CREATE INDEX claim_binding_sheet ON claim_binding(sheet_id, claim_id, id);
INSERT INTO claim_binding (sheet_id, claim_id, stated, stated_due, set_on)
    SELECT sheet_id, id, TRUE, stated_due, DATE '0001-01-01' FROM claim WHERE stated;
