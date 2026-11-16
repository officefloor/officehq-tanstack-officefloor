-- A retired person can be unretired. Each past retirement is kept: from the retirement date to the sheet's today when
-- they were unretired. No claim of theirs may be dated after the from and before the to of one.
CREATE TABLE person_retirement (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200) NOT NULL,
    person_id  BIGINT       NOT NULL REFERENCES person (id),
    from_date  DATE         NOT NULL,
    to_date    DATE         NOT NULL,
    made_on    DATE         NOT NULL
);
CREATE INDEX person_retirement_sheet ON person_retirement (sheet_id, person_id, id);

-- An unretirement is logged as a retirement date of NULL (from then on: not retired).
ALTER TABLE person_retire_log ALTER COLUMN from_date SET NULL;
