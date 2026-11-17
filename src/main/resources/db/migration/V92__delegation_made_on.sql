-- The sheet's today when the delegation was made (null: made before this was kept, so it counts on every date). As of a
-- date, a delegation made while the sheet's today was after it did not yet exist.
ALTER TABLE delegation ADD COLUMN made_on DATE;
