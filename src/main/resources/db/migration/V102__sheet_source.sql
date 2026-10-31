-- The sheet a sheet was cloned from: a grade with no rate in force on a date on this sheet nor on its parents
-- takes that sheet's (by its own rates, parents and source in turn). NULL: not a clone.
ALTER TABLE sheet ADD COLUMN source VARCHAR(200);
