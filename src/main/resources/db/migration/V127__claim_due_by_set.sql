-- Whether a steward set the claim's due-by directly: a holiday added later does not move it.
ALTER TABLE claim ADD COLUMN due_by_set BOOLEAN DEFAULT FALSE NOT NULL;
