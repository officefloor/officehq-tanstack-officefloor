-- A person's plans: an amount the claim form would take, a start date, every N days (at least 1) and
-- an end date (not before the start). Generating makes a DRAFT claim for each plan date (the start,
-- then every N days) up to the end and the sheet's today that has none yet; plan_claim records each
-- date a plan has made its claim for, so a date is made once, and a date refused is tried again.
CREATE TABLE plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    amount DECIMAL(19, 2) NOT NULL,
    start_date DATE NOT NULL,
    every_days INT NOT NULL,
    end_date DATE NOT NULL
);
CREATE TABLE plan_claim (
    plan_id BIGINT NOT NULL REFERENCES plan(id),
    claim_date DATE NOT NULL,
    claim_id BIGINT NOT NULL,
    PRIMARY KEY (plan_id, claim_date)
);
