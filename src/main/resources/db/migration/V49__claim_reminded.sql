-- An OPEN or PENDING claim may be reminded, once per sheet-today: the last day a reminder was
-- written. Null: never reminded.
ALTER TABLE public.claim ADD COLUMN reminded_on DATE;
ALTER TABLE asof.claim ADD COLUMN reminded_on DATE;
ALTER TABLE asof_hist.claim ADD COLUMN reminded_on DATE;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
