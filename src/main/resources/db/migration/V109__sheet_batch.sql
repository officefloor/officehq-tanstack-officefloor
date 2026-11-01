-- The number of the sheet's last bulk settle batch: kept on the sheet, so it stays when the claims
-- that carry it leave the sheet (a transfer takes them away), and the next batch never reuses it.
CREATE TABLE sheet_batch (
    sheet_id   VARCHAR(200) PRIMARY KEY,
    last_batch INT          NOT NULL
);

CREATE TABLE asof.sheet_batch AS SELECT * FROM public.sheet_batch WHERE FALSE;
CREATE TABLE asof_hist.sheet_batch AS SELECT * FROM public.sheet_batch WHERE FALSE;
ALTER TABLE asof_hist.sheet_batch ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.sheet_batch ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.sheet_batch ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX sheet_batch_hist_sheet ON asof_hist.sheet_batch (sheet_id, h_seq);
CREATE TRIGGER sheet_batch_history AFTER INSERT, UPDATE, DELETE ON public.sheet_batch FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
