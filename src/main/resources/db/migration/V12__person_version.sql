-- The version of a person's stored values: starts at 1 and goes up by one each time they are
-- saved, so an editor that loaded an older version is refused rather than overwriting a newer save.
ALTER TABLE person ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
