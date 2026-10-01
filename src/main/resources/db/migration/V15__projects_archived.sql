-- Archiving a project tucks it away instead of deleting it: it drops off the lists but the row is
-- retained, and a toggle can reveal archived ones again. A new boolean column, defaulting to FALSE
-- so every existing project stays active.
ALTER TABLE projects ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
