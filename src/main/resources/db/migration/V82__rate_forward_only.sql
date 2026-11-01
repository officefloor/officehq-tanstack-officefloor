-- A rate saved with a from-date on or after the sheet's today is forward-only: it applies only to
-- the claims added after it was saved. `forward_after` is the highest claim sequence there was when
-- it was saved (a claim applies it only when its own sequence is higher); null for a rate that
-- applies to every claim. Kept on each version, so a claim a forward-only version passes over keeps
-- the version that applied to it.
ALTER TABLE public.rate ADD COLUMN forward_after BIGINT;
ALTER TABLE asof.rate ADD COLUMN forward_after BIGINT;
ALTER TABLE asof_hist.rate ADD COLUMN forward_after BIGINT;
DROP TRIGGER public.rate_history;
CREATE TRIGGER rate_history AFTER INSERT, UPDATE, DELETE ON public.rate FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
ALTER TABLE public.rate_version ADD COLUMN forward_after BIGINT;

-- The older versions are read with the rates, so a sheet as it stood keeps them too.
CREATE TABLE asof.rate_version AS SELECT * FROM public.rate_version WHERE FALSE;
CREATE TABLE asof_hist.rate_version AS SELECT * FROM public.rate_version WHERE FALSE;
ALTER TABLE asof_hist.rate_version ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.rate_version ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.rate_version ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX rate_version_hist_sheet ON asof_hist.rate_version (sheet_id, h_seq);
CREATE TRIGGER rate_version_history AFTER INSERT, UPDATE, DELETE ON public.rate_version FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

-- A claim's sequence: the order claims were added in, across every sheet. Null for its own id; a
-- clone of a sheet keeps each claim's, so its rates apply to its claims as they did.
ALTER TABLE public.claim ADD COLUMN added_seq BIGINT;
ALTER TABLE asof.claim ADD COLUMN added_seq BIGINT;
ALTER TABLE asof_hist.claim ADD COLUMN added_seq BIGINT;
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
