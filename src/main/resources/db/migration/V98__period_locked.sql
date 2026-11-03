-- Whether a period is locked by a steward: a locked period cannot be reopened.
ALTER TABLE period ADD COLUMN locked BOOLEAN DEFAULT FALSE NOT NULL;
