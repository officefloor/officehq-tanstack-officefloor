-- Orders a person's statements (null: by id, as taken). Statements that move to a person with a merge or a transfer
-- are ordered before that person's own, so their last statement stays theirs.
ALTER TABLE statement ADD COLUMN ord BIGINT;
