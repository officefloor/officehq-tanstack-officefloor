-- A person may have several holds over time, each from its from-date to its to-date (null: open-ended), both
-- inclusive. A hold leaves out of the live totals only the person's OPEN and PENDING claims dated within it that no
-- statement binds. A person held before keeps a hold from the beginning of time (null from-date).
CREATE TABLE person_hold (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES person (id),
    from_date DATE,
    to_date   DATE
);
CREATE INDEX person_hold_person ON person_hold (person_id, id);
INSERT INTO person_hold (person_id, from_date, to_date) SELECT id, NULL, NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
