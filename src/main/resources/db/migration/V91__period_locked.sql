-- A locked period: a steward has locked it, so it cannot be reopened until a steward unlocks it.
ALTER TABLE period ADD COLUMN locked BOOLEAN DEFAULT FALSE NOT NULL;
