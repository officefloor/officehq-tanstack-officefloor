-- A person's plans: an amount every N days from a start date to an end date, in the order they were
-- added (id). Generating makes a DRAFT claim for each plan date that has none yet.
CREATE TABLE plan (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)   NOT NULL,
    person_id  BIGINT         NOT NULL,
    amount     DECIMAL(19, 2) NOT NULL,
    start_date DATE           NOT NULL,
    every_days INT            NOT NULL,
    end_date   DATE           NOT NULL
);
-- The plan dates a plan has made its claim for: each date at most once.
CREATE TABLE plan_claim (
    plan_id    BIGINT NOT NULL,
    claim_date DATE   NOT NULL,
    claim_id   BIGINT NOT NULL,
    PRIMARY KEY (plan_id, claim_date)
);
