-- Each pool member's cut for a closed (or soft-closed) period, fixed when the period was closed: the
-- cut as the sheet stood at that moment. member_key is the person's id, or pool:<name> for a member
-- pool; cut is null when the member had none. Reopening deletes the rows; closing again fixes them afresh.
CREATE TABLE period_pool_cut (
    sheet_id    VARCHAR(200)  NOT NULL,
    period_name VARCHAR(200)  NOT NULL,
    pool        VARCHAR(200)  NOT NULL,
    member_key  VARCHAR(220)  NOT NULL,
    cut         DECIMAL(14,2),
    PRIMARY KEY (sheet_id, period_name, pool, member_key)
);

CREATE TABLE asof.period_pool_cut AS SELECT * FROM public.period_pool_cut WHERE FALSE;
CREATE TABLE asof_hist.period_pool_cut AS SELECT * FROM public.period_pool_cut WHERE FALSE;
ALTER TABLE asof_hist.period_pool_cut ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.period_pool_cut ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.period_pool_cut ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX period_pool_cut_hist_sheet ON asof_hist.period_pool_cut (sheet_id, h_seq);
CREATE TRIGGER period_pool_cut_history AFTER INSERT, UPDATE, DELETE ON public.period_pool_cut FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
