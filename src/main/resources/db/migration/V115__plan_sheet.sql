-- A plan may name one of its sheet's descendants (null: its own sheet). Its claims are then made for
-- the person of the same name on that sheet (none when there is no such person), by that sheet's
-- rules and written in that sheet's audit; plan_claim still counts what the plan made.
ALTER TABLE plan ADD COLUMN target_sheet_id VARCHAR(200);
