-- The sheet's parent sheet: a grade with no rate in force on a date on this sheet takes the parent's
-- (then the parent's parent's, up to three levels up). NULL: none. Not dated.
ALTER TABLE sheet ADD COLUMN parent VARCHAR(200);
