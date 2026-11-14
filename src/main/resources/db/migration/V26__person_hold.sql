-- A hold is dated: it runs from its from-date to its to-date (none: open-ended), and a person may
-- have several over time. Only a person's OPEN claims dated within a hold count in no live total;
-- a claim bound by a statement still counts whatever its date. A person held before holds were
-- dated keeps a hold from the beginning of time.
CREATE TABLE person_hold (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    from_date DATE         NOT NULL,
    to_date   DATE,
    held_by   VARCHAR(200) NOT NULL,
    held_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX person_hold_person ON person_hold (sheet_id, person_id, id);
INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, held_by)
    SELECT sheet_id, id, DATE '0001-01-01', NULL, created_by FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
