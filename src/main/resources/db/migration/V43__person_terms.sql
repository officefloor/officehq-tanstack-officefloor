-- A person's own terms, in days: when set, a claim of theirs is due-by its date plus these, ahead of
-- their grade's terms and the sheet's. NULL: they have none.
ALTER TABLE person ADD COLUMN terms INT;
