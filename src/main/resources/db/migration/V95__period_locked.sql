-- A steward can lock a period: while locked it cannot be reopened. False: not locked.
ALTER TABLE period ADD COLUMN locked BOOLEAN DEFAULT FALSE NOT NULL;
