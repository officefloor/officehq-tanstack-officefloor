-- A plan may name one of its sheet's descendants: its claims are then made for the person of the same name
-- on that sheet, by that sheet's rules. NULL makes them on the plan's own sheet, for its own person.
ALTER TABLE plan ADD COLUMN target_sheet VARCHAR(200);
