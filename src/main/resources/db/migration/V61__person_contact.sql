-- A person's contact: trimmed and lower-cased, unique on the sheet; null when they have none.
ALTER TABLE person ADD COLUMN contact VARCHAR(200);
