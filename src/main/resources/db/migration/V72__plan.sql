-- A person's plans, in the order added (by id): an amount (more than 0.00, two decimals), a start
-- date, every N days (at least 1) and an end date (not before the start).
CREATE TABLE plan (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)  NOT NULL,
    person_id  BIGINT        NOT NULL REFERENCES person (id),
    amount     DECIMAL(19,2) NOT NULL,
    start_date DATE          NOT NULL,
    every_days INT           NOT NULL,
    end_date   DATE          NOT NULL
);
CREATE INDEX plan_sheet ON plan (sheet_id, id);

-- The plan dates a plan has made its claim for: each at most once, so a generate never makes it again.
CREATE TABLE plan_claim (
    sheet_id VARCHAR(200) NOT NULL,
    plan_id  BIGINT       NOT NULL REFERENCES plan (id),
    on_date  DATE         NOT NULL,
    PRIMARY KEY (plan_id, on_date)
);
CREATE INDEX plan_claim_sheet ON plan_claim (sheet_id, plan_id);

CREATE TABLE asof.plan AS SELECT * FROM public.plan WHERE FALSE;
CREATE TABLE asof_hist.plan AS SELECT * FROM public.plan WHERE FALSE;
ALTER TABLE asof_hist.plan ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.plan ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.plan ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX plan_hist_sheet ON asof_hist.plan (sheet_id, h_seq);
CREATE TRIGGER plan_history AFTER INSERT, UPDATE, DELETE ON public.plan FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.plan_claim AS SELECT * FROM public.plan_claim WHERE FALSE;
CREATE TABLE asof_hist.plan_claim AS SELECT * FROM public.plan_claim WHERE FALSE;
ALTER TABLE asof_hist.plan_claim ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.plan_claim ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.plan_claim ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX plan_claim_hist_sheet ON asof_hist.plan_claim (sheet_id, h_seq);
CREATE TRIGGER plan_claim_history AFTER INSERT, UPDATE, DELETE ON public.plan_claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
