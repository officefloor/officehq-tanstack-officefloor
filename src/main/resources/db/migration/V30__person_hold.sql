-- A person's holds: each runs from its from-date to its to-date (both inclusive; no to-date: open-ended).
-- A person may have several over time. While a hold covers a claim's date, that claim, if open (OPEN or
-- PENDING) and not bound by a statement, counts in no live total. A person held before holds were dated
-- keeps a hold from the beginning of time (a null from-date).
CREATE TABLE person_hold (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    from_date DATE,
    to_date   DATE
);
CREATE INDEX person_hold_person ON person_hold (person_id, id);
INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) SELECT sheet_id, id, NULL, NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
