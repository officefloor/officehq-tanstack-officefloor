-- A person's version: raised by one on every save, so an editor can tell when someone else saved in between.
ALTER TABLE person ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
