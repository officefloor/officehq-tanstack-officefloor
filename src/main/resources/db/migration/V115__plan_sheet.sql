-- A plan may name one of its sheet's descendants: its claims are then made for the person of the same name on that
-- sheet (none when there is no such person), by that sheet's rules and in that sheet's audit. Empty: the plan's own sheet.
ALTER TABLE plan ADD COLUMN target_sheet VARCHAR(100);
