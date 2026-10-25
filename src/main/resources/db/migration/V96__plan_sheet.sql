-- A plan may name one of its sheet's descendants (a child, a child's child, and so on): its claims are then generated
-- for the person of the same name on that sheet, by that sheet's rules. Null for a plan on its own sheet.
ALTER TABLE plan ADD COLUMN target_sheet_id VARCHAR(200);
