-- The version of a person, raised on every edit. An edit names the version its form loaded and is
-- refused when the person has been saved since.
ALTER TABLE person ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
