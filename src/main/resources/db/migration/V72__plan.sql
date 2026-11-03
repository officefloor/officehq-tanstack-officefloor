-- A person's plans: an amount (more than 0.00, two decimals), a start date, an interval in days (1 or
-- more) and an end date (not before the start). Generating makes a DRAFT claim for each plan date (the
-- start, then every N days, up to the end and the sheet's today) that has none yet; plan_claim keeps the
-- dates a plan has made a claim for. Listed in the order they were added (by id).
CREATE TABLE plan (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    person_id  BIGINT         NOT NULL REFERENCES person (id),
    amount     DECIMAL(14, 2) NOT NULL CHECK (amount > 0),
    start_date DATE           NOT NULL,
    every_days INT            NOT NULL CHECK (every_days >= 1),
    end_date   DATE           NOT NULL
);
CREATE INDEX plan_sheet ON plan (sheet_id, id);

CREATE TABLE plan_claim (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    plan_id  BIGINT       NOT NULL REFERENCES plan (id),
    on_date  DATE         NOT NULL,
    claim_id BIGINT       NOT NULL,
    PRIMARY KEY (plan_id, on_date)
);
CREATE INDEX plan_claim_sheet ON plan_claim (sheet_id);
