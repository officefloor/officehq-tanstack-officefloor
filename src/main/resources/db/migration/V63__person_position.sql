-- A person's place in the sheet's list: the list runs by it, lowest first. Null (a person added since, or before
-- this was kept) places them by their id, so a new person goes to the end; moving a person swaps the places of
-- the two people.
ALTER TABLE person ADD COLUMN position BIGINT;
