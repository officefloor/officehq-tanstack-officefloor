-- A person may have terms of their own, in days (null: none). A new claim's due-by is its date plus the person's own
-- terms, else their grade's, else the sheet's.
ALTER TABLE person ADD COLUMN terms INTEGER;
