-- The sheet a sheet was cloned from: a grade with no rate in force on a date on this sheet nor on
-- its parents takes that sheet's (and so on up its parents and the sheet it was cloned from). Null: none.
ALTER TABLE sheet ADD COLUMN cloned_from VARCHAR(200);
