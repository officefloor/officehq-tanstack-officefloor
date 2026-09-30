-- A project's lifecycle status: ACTIVE while worked on, ON_HOLD when paused, FINISHED when done.
-- Shown on the project's row and chosen when creating one. Existing projects default to ACTIVE.
ALTER TABLE projects ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
