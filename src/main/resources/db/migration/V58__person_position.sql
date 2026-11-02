-- A person's place in the sheet's list: people are listed by it, lowest first. New people go to the end (the
-- sheet's highest plus one); moving a person up or down swaps their position with the neighbour's.
ALTER TABLE person ADD COLUMN list_position BIGINT;
UPDATE person SET list_position = id;
ALTER TABLE person ALTER COLUMN list_position SET NOT NULL;
CREATE INDEX person_sheet_position ON person (sheet, list_position);
