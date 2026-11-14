-- The sheet a sheet was cloned from (null: not a clone): a grade with no rate in force on the sheet nor its parents
-- takes the rate of that sheet (and so on up its parents and the sheet it was cloned from).
ALTER TABLE sheet ADD COLUMN cloned_from VARCHAR(100);
