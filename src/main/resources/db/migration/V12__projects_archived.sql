-- Archiving a project: instead of deleting it, mark it archived so it drops off the lists but is
-- retained. Existing projects default to not archived. A toggle on the projects page reveals the
-- archived ones; the client's page never shows them.
ALTER TABLE projects ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
