-- A claim's tags, lower-cased, sorted and comma separated ("a,b"); null when it has none.
-- `tags_used` is every tag the claim has ever carried, in the same form: the sheet shows a
-- due-total for every tag ever used on it, also one an edit has since taken off.
ALTER TABLE public.claim ADD COLUMN tags VARCHAR(2000);
ALTER TABLE public.claim ADD COLUMN tags_used VARCHAR(2000);
ALTER TABLE asof.claim ADD COLUMN tags VARCHAR(2000);
ALTER TABLE asof.claim ADD COLUMN tags_used VARCHAR(2000);
ALTER TABLE asof_hist.claim ADD COLUMN tags VARCHAR(2000);
ALTER TABLE asof_hist.claim ADD COLUMN tags_used VARCHAR(2000);
-- The history trigger keeps the column list it was created with: create it again to see the new ones.
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
