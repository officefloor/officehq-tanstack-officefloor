-- A person's holds are dated: each runs from its from-date (null: from the beginning of time) to its to-date
-- (null: open-ended), both included, and a person may have several over time. Only their OPEN claims dated within a
-- hold, and not bound by a statement, are left out of the live totals. A person held before keeps an open-ended
-- hold from the beginning of time.
CREATE TABLE person_hold (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    person_id BIGINT       NOT NULL REFERENCES person(id),
    from_date DATE,
    to_date   DATE
);
CREATE INDEX person_hold_sheet ON person_hold(sheet_id, person_id);
INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) SELECT sheet_id, id, NULL, NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
