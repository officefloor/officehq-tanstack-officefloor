-- The version of a person: 0 when added, one more on each stored edit. An edit names the version
-- it was made from and is refused when the person has been saved since.
ALTER TABLE person ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
