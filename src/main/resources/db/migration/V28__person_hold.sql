-- A person's holds, each from a date to an optional to-date (open-ended when null), in the order
-- they were added (seq). A person may have several over time. Only their OPEN (and PENDING) claims
-- dated within a hold, and not bound by a statement, are left out of the live totals. A person
-- held before holds had dates is held from the beginning of time.
CREATE TABLE person_hold (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE
);
INSERT INTO person_hold (sheet_id, person_id, from_date, to_date)
    SELECT sheet_id, id, DATE '0001-01-01', NULL FROM person WHERE held;
ALTER TABLE person DROP COLUMN held;
