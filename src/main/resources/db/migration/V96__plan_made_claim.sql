-- The claim each plan date made (on claim_sheet, the plan's sheet or the descendant it names), so a change of the
-- plan's amount reaches the claims it generated that are still DRAFT. Null for dates made before it was kept.
ALTER TABLE plan_made ADD COLUMN claim_sheet VARCHAR(255);
ALTER TABLE plan_made ADD COLUMN claim_id BIGINT;
