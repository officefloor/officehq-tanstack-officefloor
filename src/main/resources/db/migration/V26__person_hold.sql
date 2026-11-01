-- A person's holds: each from its from-date to its to-date (both included; a null to-date never ends), and a person
-- may have several over time, listed in the order they were added (by id). Only the person's OPEN (and PENDING)
-- claims dated within a hold, and not bound by a statement, are left out of the live totals. A person held before
-- holds were dated is held from the beginning of time.
CREATE TABLE person_hold (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id BIGINT NOT NULL,
    from_date DATE   NOT NULL,
    to_date   DATE
);
CREATE INDEX person_hold_person ON person_hold (person_id, id);
INSERT INTO person_hold (person_id, from_date, to_date) SELECT id, DATE '0001-01-01', NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
