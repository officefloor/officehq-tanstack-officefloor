-- A person's place in the sheet's list: people are listed (and taken in list order) by position, lowest first. New
-- people, and people transferred in, go to the end; moving a person up or down swaps positions with their neighbour.
-- People already on a sheet keep the order they were added in.
ALTER TABLE person ADD COLUMN position BIGINT;
UPDATE person SET position = id;
CREATE INDEX person_sheet_position ON person (sheet_id, position, id);
