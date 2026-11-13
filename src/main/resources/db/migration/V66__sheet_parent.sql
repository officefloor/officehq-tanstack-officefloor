-- The sheet's parent sheet: a grade with no rate in force on a date here takes the parent's (then its parent's, up to
-- three levels up); null: none. Not dated.
ALTER TABLE sheet ADD COLUMN parent_id VARCHAR(100);
