-- A locked period: only a steward locks and unlocks it, and while locked it cannot be reopened.
ALTER TABLE period ADD COLUMN locked BOOLEAN DEFAULT FALSE NOT NULL;
