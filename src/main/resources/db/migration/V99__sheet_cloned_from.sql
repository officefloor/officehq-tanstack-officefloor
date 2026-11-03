-- The sheet a sheet was cloned from (null means it was not cloned; not dated): a grade with no rate in force on a
-- date on the sheet nor on its parents takes the rate of the sheet it was cloned from, and so on up that sheet's
-- parents and source.
ALTER TABLE sheet ADD COLUMN cloned_from VARCHAR(200);
