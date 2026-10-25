-- The sheet a sheet was cloned from (null: not a clone): a grade with no rate in force on the clone nor its parents
-- takes the source's (and so on up the source's parents and source).
ALTER TABLE sheet ADD COLUMN cloned_from VARCHAR(200);
