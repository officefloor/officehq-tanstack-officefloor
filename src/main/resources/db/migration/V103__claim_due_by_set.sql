-- Whether a steward set the claim's due-by directly: a due-by set so is not re-fixed when a holiday is added.
ALTER TABLE claim ADD COLUMN due_by_set BOOLEAN DEFAULT FALSE NOT NULL;
