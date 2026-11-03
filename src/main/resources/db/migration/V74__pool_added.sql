-- A pool's place in the order pools were added to the sheet (from a sequence; a cloned sheet keeps its source's order).
CREATE SEQUENCE pool_added_seq;
ALTER TABLE pool ADD COLUMN added BIGINT DEFAULT NEXT VALUE FOR pool_added_seq;
UPDATE pool SET added = NEXT VALUE FOR pool_added_seq WHERE added IS NULL;
ALTER TABLE pool ALTER COLUMN added SET NOT NULL;
