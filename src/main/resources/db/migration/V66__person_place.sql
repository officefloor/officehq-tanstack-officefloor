-- A person's place in the sheet's list: people are listed by place, lowest first. A new person takes a place after
-- everyone's (their id); moving a person up or down swaps places with their neighbour.
ALTER TABLE person ADD COLUMN place BIGINT;
UPDATE person SET place = id;
ALTER TABLE person_merged ADD COLUMN place BIGINT BEFORE merged_on;
UPDATE person_merged SET place = id;
