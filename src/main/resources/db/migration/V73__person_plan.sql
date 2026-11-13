-- A person's plans: an amount (0.00 or more, two decimals) every N days (1 or more) from a start date to an end date
-- (not before the start). Generating makes a DRAFT claim for each plan date up to the end and the sheet's today;
-- person_plan_date keeps the dates a plan has made a claim for, so each is made once.
CREATE TABLE person_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    amount DECIMAL(15, 2) NOT NULL,
    start_date DATE NOT NULL,
    every_days INT NOT NULL,
    end_date DATE NOT NULL
);
CREATE INDEX person_plan_sheet ON person_plan (sheet_id);
CREATE TABLE person_plan_date (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    plan_id BIGINT NOT NULL REFERENCES person_plan (id),
    plan_date DATE NOT NULL,
    PRIMARY KEY (plan_id, plan_date)
);
CREATE INDEX person_plan_date_sheet ON person_plan_date (sheet_id);
