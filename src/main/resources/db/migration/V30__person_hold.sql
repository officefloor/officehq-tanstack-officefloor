-- A person's holds over time: each from a date on, to a date (inclusive; null: open-ended). Their OPEN claims
-- dated within a hold count in no live total, unless a statement binds them; their other claims still count.
-- Replaces the single held flag: a person held before is held from the beginning of time.
CREATE TABLE person_hold (
    person_id BIGINT NOT NULL REFERENCES person (id),
    from_date DATE NOT NULL,
    to_date DATE
);
CREATE INDEX person_hold_person ON person_hold (person_id);
INSERT INTO person_hold (person_id, from_date, to_date) SELECT id, DATE '0001-01-01', NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
