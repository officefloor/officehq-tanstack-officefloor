-- The descendant sheet a plan makes its claims on (null: the plan's own sheet): its claims are made for the person of
-- the plan's person's name on that sheet, by that sheet's rules.
ALTER TABLE person_plan ADD COLUMN target_sheet_id VARCHAR(100);
