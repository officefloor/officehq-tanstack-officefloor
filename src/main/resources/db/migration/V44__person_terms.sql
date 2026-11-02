-- A person's own terms: the days after a claim's date that it is due by, ahead of their grade's and
-- the sheet's terms. Null when they have none.
ALTER TABLE person ADD COLUMN terms INT;
