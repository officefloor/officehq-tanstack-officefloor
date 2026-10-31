-- A person's own terms, in days: a claim of theirs is due by its date plus them, ahead of their
-- grade's and the sheet's terms. Null: none of their own.
ALTER TABLE person ADD COLUMN terms INT;
