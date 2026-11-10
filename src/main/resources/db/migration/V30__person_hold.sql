-- A person's holds: each from a date until an optional to-date (null: open-ended), both inclusive.
-- A person may have several over time. While a claim's date is within one of its person's holds,
-- an OPEN claim not bound by a statement counts in no total. Replaces person.held: a person held
-- before is given one open-ended hold from the beginning of time.
CREATE TABLE person_hold (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    from_date DATE NOT NULL,
    to_date DATE
);
CREATE INDEX person_hold_sheet ON person_hold(sheet_id, person_id, id);
INSERT INTO person_hold (sheet_id, person_id, from_date, to_date)
    SELECT sheet_id, id, DATE '0001-01-01', NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
