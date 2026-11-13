-- A retired person can be unretired: their retirement is kept as a past one, from its from-date to the sheet's today
-- when they were unretired. A claim dated after the from-date and before the to-date of a past retirement is refused.
CREATE TABLE person_retirement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    from_date DATE NOT NULL,
    to_date DATE NOT NULL
);
CREATE INDEX person_retirement_sheet ON person_retirement (sheet_id, person_id);
