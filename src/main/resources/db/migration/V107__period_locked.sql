-- Whether the period is locked: a locked period cannot be reopened until a steward unlocks it.
ALTER TABLE period ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE;
