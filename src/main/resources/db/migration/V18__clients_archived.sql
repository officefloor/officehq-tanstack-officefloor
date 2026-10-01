-- Archiving a client tucks it away instead of deleting it: it drops off the list and search but the
-- row is retained (nothing is lost). A new boolean column, defaulting to FALSE so every existing
-- client stays active.
ALTER TABLE clients ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
