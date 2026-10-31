-- The terms (business days) a claim's due-by was fixed with, so it can be worked out again from its
-- date when a holiday is added. Filled for the claims kept by the next migration.
ALTER TABLE public.claim ADD COLUMN due_terms INT;
ALTER TABLE asof.claim ADD COLUMN due_terms INT;
ALTER TABLE asof_hist.claim ADD COLUMN due_terms INT;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
