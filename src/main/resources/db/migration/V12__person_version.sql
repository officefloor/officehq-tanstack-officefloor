-- The version of a person, raised by one on each save, so an edit made from an older version
-- (someone else saved the person in the meantime) can be refused.
ALTER TABLE person ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
