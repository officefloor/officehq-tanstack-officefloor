-- A plan's changes of amount, oldest first (by id): changed on the sheet's today, to the new amount.
-- The plan's amount is the latest; claims already made keep theirs.
CREATE TABLE plan_change (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)  NOT NULL,
    plan_id    BIGINT        NOT NULL REFERENCES plan (id),
    changed_on DATE          NOT NULL,
    amount     DECIMAL(19,2) NOT NULL
);
CREATE INDEX plan_change_sheet ON plan_change (sheet_id, plan_id);

CREATE TABLE asof.plan_change AS SELECT * FROM public.plan_change WHERE FALSE;
CREATE TABLE asof_hist.plan_change AS SELECT * FROM public.plan_change WHERE FALSE;
ALTER TABLE asof_hist.plan_change ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.plan_change ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.plan_change ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX plan_change_hist_sheet ON asof_hist.plan_change (sheet_id, h_seq);
CREATE TRIGGER plan_change_history AFTER INSERT, UPDATE, DELETE ON public.plan_change FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
