-- The sheet a sheet was cloned from (null: not a clone). A grade with no rate in force on a date on a clone nor on
-- its parents takes the rate of the sheet it was cloned from, then that sheet's parents', then its source's, and so on.
ALTER TABLE sheet ADD COLUMN cloned_from VARCHAR(100);
