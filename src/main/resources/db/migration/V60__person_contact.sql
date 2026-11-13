-- A person's contact, kept trimmed and lower-cased; null: none. No two people on a sheet share one.
ALTER TABLE person ADD COLUMN contact VARCHAR(200);
