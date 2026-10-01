-- Archiving (not deleting) a project: a project can be tucked away so it drops off the lists while
-- its row — and everything hanging off it — is kept. A boolean flag on the project carries that
-- state; existing projects default to not-archived, so the lists are unchanged until one is archived.
ALTER TABLE projects ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
