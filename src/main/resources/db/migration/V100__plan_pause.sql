-- A plan's pauses, in the order made: paused on the sheet's today it was paused, resumed on the
-- sheet's today it was resumed (null while it stays paused). A plan date on or after the day paused
-- and before the day resumed is skipped. At most one pause of a plan is open.
CREATE TABLE plan_pause (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200) NOT NULL,
    plan_id    BIGINT       NOT NULL REFERENCES plan (id),
    paused_on  DATE         NOT NULL,
    resumed_on DATE
);
CREATE INDEX plan_pause_sheet ON plan_pause (sheet_id, plan_id);

CREATE TABLE asof.plan_pause AS SELECT * FROM public.plan_pause WHERE FALSE;
CREATE TABLE asof_hist.plan_pause AS SELECT * FROM public.plan_pause WHERE FALSE;
ALTER TABLE asof_hist.plan_pause ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.plan_pause ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.plan_pause ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX plan_pause_hist_sheet ON asof_hist.plan_pause (sheet_id, h_seq);
CREATE TRIGGER plan_pause_history AFTER INSERT, UPDATE, DELETE ON public.plan_pause FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
