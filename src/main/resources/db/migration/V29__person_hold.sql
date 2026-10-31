-- A person may have several holds over time, each from a date and, optionally, to a date (both
-- inclusive; no to-date: open-ended). Only their OPEN claims dated within a hold are left out of the
-- live totals; a claim bound by a statement stays in whatever its date. A person held before keeps
-- one open-ended hold from the beginning of time.
CREATE TABLE person_hold (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL REFERENCES person (id),
    from_date DATE         NOT NULL,
    to_date   DATE
);
CREATE INDEX person_hold_sheet ON person_hold (sheet_id, person_id, id);
INSERT INTO person_hold (sheet_id, person_id, from_date) SELECT sheet_id, id, DATE '0001-01-01' FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
