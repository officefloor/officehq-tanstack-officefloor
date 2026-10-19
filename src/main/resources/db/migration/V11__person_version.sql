-- The version of a person: starts at 0 and goes up by one each time the person is saved. An edit
-- carries the version its form loaded, and is refused when the person has been saved since.
ALTER TABLE person ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
