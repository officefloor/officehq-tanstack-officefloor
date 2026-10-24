-- A person's contact: trimmed and lower-cased; at most one person on a sheet has a given contact. Null (blank)
-- means they have none.
ALTER TABLE person ADD COLUMN contact VARCHAR(255);
