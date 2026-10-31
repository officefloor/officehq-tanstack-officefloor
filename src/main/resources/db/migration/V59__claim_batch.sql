-- The number of the bulk settle (numbered 1, 2, ... on its sheet) that last settled a claim (or sent it
-- PENDING); null for a claim never so settled, or settled one at a time since.
ALTER TABLE public.claim ADD COLUMN batch INTEGER;
ALTER TABLE asof.claim ADD COLUMN batch INTEGER;
ALTER TABLE asof_hist.claim ADD COLUMN batch INTEGER;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
