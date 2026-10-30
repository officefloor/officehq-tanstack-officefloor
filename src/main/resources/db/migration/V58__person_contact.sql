-- A person's contact, trimmed and lower-cased; null for none. No two people on a sheet share one.
ALTER TABLE person ADD COLUMN contact VARCHAR(255);
-- Each contact recorded for a person, alongside their name and own terms.
ALTER TABLE person_history ADD COLUMN contact VARCHAR(255);
