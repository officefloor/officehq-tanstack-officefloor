-- A person's plans: an amount, a start date, every N days (at least 1) and an end date. Generating makes a DRAFT claim
-- for each plan date (the start, then every N days) up to the end and the sheet's today; plan_claim keeps each date a
-- plan has made its claim for, so a date is made once only, and a date the claim form refused is tried again next time.
CREATE TABLE plan (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(255)   NOT NULL,
    person_id  BIGINT         NOT NULL REFERENCES person (id),
    amount     DECIMAL(19, 2) NOT NULL,
    start_date DATE           NOT NULL,
    every_days INT            NOT NULL,
    end_date   DATE           NOT NULL
);
CREATE INDEX plan_sheet ON plan (sheet_id);
CREATE TABLE plan_claim (
    plan_id  BIGINT NOT NULL REFERENCES plan (id),
    on_date  DATE   NOT NULL,
    claim_id BIGINT NOT NULL,
    PRIMARY KEY (plan_id, on_date)
);
