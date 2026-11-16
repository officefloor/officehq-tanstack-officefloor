-- A person may have several holds over time, each from a date until a date (both included; no to-date: open-ended).
-- Only their OPEN or PENDING claims dated within a hold, and not bound by a statement, are left out of the live totals.
-- A person held before holds were dated keeps an open-ended hold from the earliest date, so nothing they had changes.
CREATE TABLE person_hold (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200) NOT NULL,
    person_id  BIGINT       NOT NULL REFERENCES person (id),
    from_date  DATE         NOT NULL,
    to_date    DATE
);
CREATE INDEX person_hold_sheet ON person_hold (sheet_id, person_id);
INSERT INTO person_hold (sheet_id, person_id, from_date, to_date)
    SELECT sheet_id, id, DATE '0001-01-01', NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
