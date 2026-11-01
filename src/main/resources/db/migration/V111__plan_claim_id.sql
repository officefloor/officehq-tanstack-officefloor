-- The claim a plan made for a plan date, so a change of the plan's amount reaches its DRAFT claims.
-- Null for a plan date made before this was kept.
ALTER TABLE public.plan_claim ADD COLUMN claim_id BIGINT;
ALTER TABLE asof.plan_claim ADD COLUMN claim_id BIGINT;
ALTER TABLE asof_hist.plan_claim ADD COLUMN claim_id BIGINT;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.plan_claim_history;
CREATE TRIGGER plan_claim_history AFTER INSERT, UPDATE, DELETE ON public.plan_claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
