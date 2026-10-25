-- Whether a steward set the claim's due-by directly: such a due-by stays as set (a holiday added later does not move it).
ALTER TABLE claim ADD COLUMN due_by_set BOOLEAN DEFAULT FALSE NOT NULL;
