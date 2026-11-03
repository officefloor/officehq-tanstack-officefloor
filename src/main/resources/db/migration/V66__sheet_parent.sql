-- A sheet's parent sheet (null means none; not dated): a grade with no rate in force on a date on the sheet
-- takes its parent's rate for that grade and date, then the parent's parent's, up to three levels up.
ALTER TABLE sheet ADD COLUMN parent_id VARCHAR(200);
