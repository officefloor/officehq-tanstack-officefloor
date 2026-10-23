-- A steward may set a claim's due-by directly. A due-by set so stays as set: adding a holiday does not re-fix it.
ALTER TABLE claim ADD COLUMN due_by_set BOOLEAN DEFAULT FALSE NOT NULL;
