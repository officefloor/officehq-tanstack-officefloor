-- A person's place in the sheet's list: people are listed (and everything done in list order works) by position,
-- lowest first. A new person goes to the end; moving a person up or down swaps their position with the neighbour's.
ALTER TABLE person ADD COLUMN position BIGINT;
UPDATE person SET position = id;
ALTER TABLE person ALTER COLUMN position SET NOT NULL;
