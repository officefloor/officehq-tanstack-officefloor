-- A retirement a person was unretired from: from its from-date to the sheet's today when they were
-- unretired, oldest first. No claim dated after the from and before the to can be added to them.
CREATE TABLE person_past_retirement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    from_date DATE NOT NULL,
    to_date DATE NOT NULL
);
CREATE INDEX person_past_retirement_sheet ON person_past_retirement(sheet_id, person_id, id);
