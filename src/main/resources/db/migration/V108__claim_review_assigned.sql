-- The user a steward assigned a REVIEW claim to: kept once the claim leaves REVIEW. Null when none.
ALTER TABLE public.claim ADD COLUMN review_assigned VARCHAR(255);
ALTER TABLE asof.claim ADD COLUMN review_assigned VARCHAR(255);
ALTER TABLE asof_hist.claim ADD COLUMN review_assigned VARCHAR(255);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
