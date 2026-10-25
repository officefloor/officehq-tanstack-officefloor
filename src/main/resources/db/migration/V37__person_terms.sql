-- A person's own terms: a whole number of days, null for none. A claim added for the person is due by
-- its date plus these terms, else their grade's, else the sheet's.
ALTER TABLE person ADD COLUMN terms INT;
