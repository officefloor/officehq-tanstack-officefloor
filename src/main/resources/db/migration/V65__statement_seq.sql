-- The order of a person's statements: the one with the highest seq is their last. A statement taken is placed after
-- the person's others; statements that move with a merge or transfer onto a person who has their own are placed
-- before them, so the receiving person's last statement stays theirs.
ALTER TABLE statement ADD COLUMN seq BIGINT;
UPDATE statement SET seq = id;
ALTER TABLE statement ALTER COLUMN seq SET NOT NULL;
