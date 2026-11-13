-- A plan may name one of its sheet's descendants (target_sheet, null for none): its claims are then generated
-- for the person of the same name on that sheet, by that sheet's rules and recorded in its audit.
ALTER TABLE plan ADD COLUMN target_sheet VARCHAR(255);
