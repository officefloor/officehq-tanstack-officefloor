-- A person's own terms: the days after its date by which their claim is due, ahead of their grade's and the
-- sheet's; null when they have none.
ALTER TABLE person ADD COLUMN terms INT;
