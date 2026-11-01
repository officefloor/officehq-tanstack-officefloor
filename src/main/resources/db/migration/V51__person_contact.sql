-- A person's contact: trimmed and lower-cased, not shared with another person on the sheet; null when they have none.
ALTER TABLE person ADD COLUMN contact VARCHAR(255);
