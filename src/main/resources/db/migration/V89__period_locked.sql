-- A locked period: a steward locks it, and it cannot be reopened until a steward unlocks it.
ALTER TABLE period ADD COLUMN locked BOOLEAN DEFAULT FALSE NOT NULL;
