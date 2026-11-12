-- A person's past retirements, each from the date they were retired from to the day they were unretired
-- (the sheet's today then). No claim of theirs dated after the from-date and before the to-date may be added.
CREATE TABLE person_retirement (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255) NOT NULL,
    person_id BIGINT       NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL
);
CREATE INDEX person_retirement_sheet ON person_retirement (sheet_id, person_id);
