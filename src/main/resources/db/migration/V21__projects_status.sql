-- A project carries a lifecycle status so its row can show whether it is being worked on, parked,
-- or done: ACTIVE (in progress), ON_HOLD (paused) or FINISHED (completed). New projects default to
-- ACTIVE; existing rows adopt the same default via the column default.
ALTER TABLE projects ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
