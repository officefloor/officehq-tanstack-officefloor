-- A person's holds: each from its from-date to its to-date (null: no end), both inclusive. A person
-- may have several over time. Only their OPEN (or PENDING) claims dated within a hold, and not bound
-- by a statement, are left out of the live totals. A person held before holds were dated is held
-- from the beginning of time (0001-01-01) with no end.
CREATE TABLE person_hold (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255) NOT NULL,
    person_id BIGINT       NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE
);
CREATE INDEX person_hold_sheet ON person_hold (sheet_id, person_id);
INSERT INTO person_hold (sheet_id, person_id, from_date, to_date)
    SELECT sheet_id, id, DATE '0001-01-01', NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
