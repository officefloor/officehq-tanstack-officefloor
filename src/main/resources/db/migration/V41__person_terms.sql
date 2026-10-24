-- A person's own terms: a claim of theirs is due by its date plus this many days, ahead of their grade's terms and
-- the sheet's. Null (blank) means they have none.
ALTER TABLE person ADD COLUMN terms INT;
