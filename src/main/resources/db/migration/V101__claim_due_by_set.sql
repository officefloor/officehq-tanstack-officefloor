-- Whether a steward set the claim's due-by directly. A due-by set this way stays as set: adding a
-- holiday later does not work it out again.
ALTER TABLE claim ADD COLUMN due_by_set BOOLEAN DEFAULT FALSE NOT NULL;
