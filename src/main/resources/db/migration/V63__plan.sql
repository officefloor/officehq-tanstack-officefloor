-- A person's plans: an amount the claim form accepts (0.00 or more), a start date, every N days (a whole number, at
-- least 1) and an end date not before the start. Generating makes a DRAFT claim per plan date (the start, then every
-- N days) up to the end and the sheet's today that has none yet; plan_claim notes each date a claim was made for.
CREATE TABLE plan (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    person_id  BIGINT         NOT NULL REFERENCES person (id),
    amount     DECIMAL(14, 2) NOT NULL,
    start_date DATE           NOT NULL,
    every_days INT            NOT NULL,
    end_date   DATE           NOT NULL
);
CREATE INDEX plan_sheet ON plan (sheet_id, id);

CREATE TABLE plan_claim (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    plan_id   BIGINT       NOT NULL REFERENCES plan (id),
    plan_date DATE         NOT NULL
);
CREATE UNIQUE INDEX plan_claim_plan_date ON plan_claim (plan_id, plan_date);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.plan AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM plan t WHERE FALSE;
CREATE INDEX hist_plan_snapshot ON hist.plan (snapshot_id);
CREATE TABLE asof.plan AS SELECT * FROM plan WHERE FALSE;
CREATE TABLE hist.plan_claim AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM plan_claim t WHERE FALSE;
CREATE INDEX hist_plan_claim_snapshot ON hist.plan_claim (snapshot_id);
CREATE TABLE asof.plan_claim AS SELECT * FROM plan_claim WHERE FALSE;
