-- Where a statement stands among its person's (null: by its id). A statement moved to another person by a merge or a
-- transfer is placed before that person's own, so their last statement stays theirs.
ALTER TABLE statement ADD COLUMN place BIGINT;
