-- A person's own terms: the whole days after its date a claim of theirs is due by, ahead of their
-- grade's and the sheet's terms. Null when they have none.
ALTER TABLE person ADD COLUMN terms INT;
