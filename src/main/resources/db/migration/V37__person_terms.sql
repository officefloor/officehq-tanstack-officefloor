-- A person may have their own terms (whole days; null: none): a claim's due-by is fixed at
-- creation from them, before the terms of their grade and the sheet's.
ALTER TABLE person ADD COLUMN terms INT;
