-- A claim's external reference, as typed (spaces trimmed); null when it has none. Unique on its
-- sheet without regard to case.
ALTER TABLE public.claim ADD COLUMN ref VARCHAR(200);
ALTER TABLE asof.claim ADD COLUMN ref VARCHAR(200);
ALTER TABLE asof_hist.claim ADD COLUMN ref VARCHAR(200);
CREATE INDEX claim_ref ON public.claim (sheet_id, ref);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
