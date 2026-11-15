-- A person's past retirements, oldest first: each ran from its from-date to the sheet's today when
-- they were unretired (to_date). A claim dated after the from-date and before the to-date is refused,
-- as one dated after a retirement is.
CREATE TABLE person_retirement (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL
);
CREATE INDEX person_retirement_person ON person_retirement (sheet_id, person_id, id);

-- An unretirement is kept in the retirement history as a row with no date: as of it, not retired.
ALTER TABLE person_retired_history ALTER COLUMN retired_from SET NULL;
