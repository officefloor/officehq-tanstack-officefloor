-- A project moves through a lifecycle: it is active, on hold, or finished. A text status column on
-- the project carries that state; existing projects default to ACTIVE, so the lists are unchanged
-- until one is moved on. The three allowed values are enforced by the app (CreateProject).
ALTER TABLE projects ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
