-- A person's holds, each from a from-date to an optional to-date (null: no end), both inclusive; a person may
-- have several over time. Only their open claims dated within a hold, and not bound by a statement, are left out
-- of the live totals. A person held before holds had dates is held from the beginning of time (null from-date).
CREATE TABLE person_hold (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    from_date DATE,
    to_date DATE
);
CREATE INDEX person_hold_sheet ON person_hold (sheet_id, person_id);
INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) SELECT sheet_id, id, NULL, NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
