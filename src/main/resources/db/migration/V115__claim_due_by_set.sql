-- A claim whose due-by a steward set directly: it stays as set (a holiday added later does not re-fix it).
ALTER TABLE claim ADD COLUMN due_by_set BOOLEAN DEFAULT FALSE NOT NULL;
