-- Whether a steward has locked the period. A locked period cannot be reopened until it is unlocked.
ALTER TABLE period ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE;
