-- A person's plans: an amount, a start date, every N days (1 or more) and an end date, kept in the order added. A
-- generate makes a DRAFT claim for each plan date that has none yet; plan_claim records the dates a plan has made a
-- claim for, so a date is made once and how many a plan has made is known. added_on (a plan) and
-- made_on (a claim it made) are the sheet's today then, so the plans can be read as of a date.
CREATE TABLE plan (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)   NOT NULL,
    person_id  BIGINT         NOT NULL,
    amount     DECIMAL(17, 2) NOT NULL,
    start_date DATE           NOT NULL,
    every_days INT            NOT NULL,
    end_date   DATE           NOT NULL,
    added_on   DATE
);
CREATE INDEX plan_sheet ON plan (sheet_id, id);

CREATE TABLE plan_claim (
    plan_id   BIGINT NOT NULL REFERENCES plan (id),
    plan_date DATE   NOT NULL,
    claim_id  BIGINT NOT NULL,
    made_on   DATE,
    PRIMARY KEY (plan_id, plan_date)
);
