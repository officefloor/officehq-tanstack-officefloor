-- The sheet's parent sheet (null: none; not dated). A grade with no rate in force on a date on this sheet takes
-- the parent's rate for it on that date, then the parent's parent's, up to three levels up.
ALTER TABLE sheet ADD COLUMN parent VARCHAR(100);
