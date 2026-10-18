-- A hold is dated: it runs from its from-date to its to-date (both inclusive; no to-date: open-ended),
-- and a person may have several holds over time. Only a person's OPEN (and PENDING) claims dated within
-- one of their holds count in no live total; their other claims count. A hold is not released: it ends
-- on its to-date. A person held before holds were dated is held from the beginning of time (no from-date).
CREATE TABLE person_hold (
    person_id BIGINT NOT NULL,
    from_date DATE,
    to_date   DATE,
    FOREIGN KEY (person_id) REFERENCES person (id)
);
CREATE INDEX person_hold_person ON person_hold (person_id);
INSERT INTO person_hold (person_id, from_date, to_date) SELECT id, NULL, NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
