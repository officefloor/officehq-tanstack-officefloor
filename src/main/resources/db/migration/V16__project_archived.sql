-- Archiving a project tucks it away instead of deleting it: the row is kept but drops off the
-- default lists. A boolean flag on project, defaulting FALSE so every existing (and newly created)
-- project starts un-archived and visible.
ALTER TABLE project ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
