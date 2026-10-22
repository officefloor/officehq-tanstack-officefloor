-- People have a position in their sheet's list: listed by it (then by id), new people going to the end. Moving a
-- person up or down swaps their position with the neighbour's. Existing people keep the order they were added in.
ALTER TABLE person ADD COLUMN list_position BIGINT;
UPDATE person SET list_position = id;
CREATE INDEX person_sheet_position ON person(sheet_id, list_position, id);
