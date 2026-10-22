-- A person's past retirements: each one ended by unretiring them, from the date they were retired from to the
-- sheet's today when they were unretired (made_on). A claim dated after the from and before the to is refused.
-- Unretiring also logs a null from_date in person_retire_log: not retired from then on.
CREATE TABLE person_retirement (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    person_id BIGINT       NOT NULL REFERENCES person(id),
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL,
    made_on   DATE
);
CREATE INDEX person_retirement_person ON person_retirement(sheet_id, person_id, id);
