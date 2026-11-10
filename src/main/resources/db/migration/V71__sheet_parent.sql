-- A sheet's parent sheet, not dated: a grade with no rate in force on a date on this sheet takes
-- the parent's for that date, then the parent's parent, up to three levels up. Null: none.
ALTER TABLE sheet ADD COLUMN parent_id VARCHAR(200);
