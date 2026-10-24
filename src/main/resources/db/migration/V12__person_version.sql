-- The version of a person: raised by one on every save. An edit names the version it loaded and
-- is refused when someone else has saved the person since.
ALTER TABLE person ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
