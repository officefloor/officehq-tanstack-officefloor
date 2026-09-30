-- A client has one main (primary) contact. Flag it on the contact row: at most one contact per
-- client should carry is_primary = TRUE (the server enforces "one wins" on each change). Existing
-- contacts default to not-primary; the seed and the set-primary endpoint set the flag.
ALTER TABLE contact ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE;
