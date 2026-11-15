-- A person's place in their sheet's list, first to last: the end of the list when added (or when they
-- arrive from another sheet), swapped with a neighbour's when moved up or down. Ties fall back to the id.
ALTER TABLE person ADD COLUMN list_pos BIGINT DEFAULT 0 NOT NULL;
UPDATE person SET list_pos = id;
