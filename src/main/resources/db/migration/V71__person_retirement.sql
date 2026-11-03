-- A person's past retirements: each ran from its from-date (the date they were retired from) to its to-date
-- (the sheet's today when they were unretired). A person may have several over time, kept oldest first (by id).
-- No claim of theirs may be dated after the from-date and before the to-date of any of them.
CREATE TABLE person_retirement (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL
);
CREATE INDEX person_retirement_person ON person_retirement (person_id, id);
-- An unretirement is kept in the history as a retirement from no date (not retired from then on).
ALTER TABLE retire_history ALTER COLUMN retired_from SET NULL;
