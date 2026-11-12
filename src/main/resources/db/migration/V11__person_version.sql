-- Each stored edit of a person moves its version on, so an editor opened on an older version is refused.
ALTER TABLE person ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
