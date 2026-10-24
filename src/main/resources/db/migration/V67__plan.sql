-- A person's plans: an amount (0.00 or more, two decimals), a start date, every N days (at least 1) and an end date
-- (not before the start). Generating makes a DRAFT claim for each plan date (the start, then every N days) up to the
-- end and the sheet's today that has none yet.
CREATE TABLE plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    amount DECIMAL(19, 2) NOT NULL,
    start_date DATE NOT NULL,
    every_days INT NOT NULL,
    end_date DATE NOT NULL
);
CREATE INDEX plan_sheet ON plan (sheet_id, id);
-- The claims a plan has made: one per plan date at most.
CREATE TABLE plan_claim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES plan (id),
    on_date DATE NOT NULL,
    claim_id BIGINT NOT NULL REFERENCES claim (id)
);
CREATE UNIQUE INDEX plan_claim_plan_date ON plan_claim (plan_id, on_date);
