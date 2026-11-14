-- A person's version: raised by every saved edit. An edit carries the version its form loaded and
-- is refused when someone else has saved the person since.
ALTER TABLE person ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
