-- The sheet's void reasons, in the order added: while a sheet has any, a claim is voided only with
-- one of them. A reason is on a sheet's list at most once.
CREATE TABLE sheet_void_reason (
    sheet_id VARCHAR(200)  NOT NULL,
    reason   VARCHAR(200)  NOT NULL,
    position BIGINT        NOT NULL,
    PRIMARY KEY (sheet_id, reason)
);

CREATE TABLE asof.sheet_void_reason AS SELECT * FROM public.sheet_void_reason WHERE FALSE;
CREATE TABLE asof_hist.sheet_void_reason AS SELECT * FROM public.sheet_void_reason WHERE FALSE;
ALTER TABLE asof_hist.sheet_void_reason ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.sheet_void_reason ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.sheet_void_reason ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX sheet_void_reason_hist_sheet ON asof_hist.sheet_void_reason (sheet_id, h_seq);
CREATE TRIGGER sheet_void_reason_history AFTER INSERT, UPDATE, DELETE ON public.sheet_void_reason FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

-- The reason a claim was voided with, from its sheet's list; null when none was given.
ALTER TABLE public.claim ADD COLUMN void_reason VARCHAR(200);
ALTER TABLE asof.claim ADD COLUMN void_reason VARCHAR(200);
ALTER TABLE asof_hist.claim ADD COLUMN void_reason VARCHAR(200);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
