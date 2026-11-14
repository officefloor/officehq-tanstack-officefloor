-- The claim each plan date made (null for those made before this was kept), so a change of the plan's amount can reach
-- the claims it generated that are still DRAFT.
ALTER TABLE person_plan_date ADD COLUMN claim_id BIGINT;
