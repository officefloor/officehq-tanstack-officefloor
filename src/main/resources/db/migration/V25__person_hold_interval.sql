-- A person's holds: each from its from-date to its optional to-date (both included; no to-date: open-ended).
-- A person may have several over time. While a claim of theirs is OPEN or PENDING, dated within one of
-- their holds and not bound by a statement, it counts in no live total. A hold ends on its to-date; there
-- is no release. People held before holds had dates keep one from the beginning of time (from_date null).
CREATE TABLE person_hold (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255) NOT NULL,
    person_id BIGINT NOT NULL,
    from_date DATE,
    to_date DATE
);
CREATE INDEX person_hold_sheet ON person_hold (sheet_id, person_id, id);
INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) SELECT sheet_id, id, NULL, NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
