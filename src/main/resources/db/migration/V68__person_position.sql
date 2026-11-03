-- A person's place in the sheet's list (smallest first): new people go to the end, and moving a person up
-- or down swaps their place with the neighbour's. People already on a sheet keep the order they were added.
ALTER TABLE person ADD COLUMN list_position BIGINT;
UPDATE person SET list_position = id;
ALTER TABLE person ALTER COLUMN list_position SET NOT NULL;
CREATE INDEX person_sheet_position ON person (sheet_id, list_position);
