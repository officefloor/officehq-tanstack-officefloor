-- A person's own terms: a claim of theirs is due by its date plus this many days, before their grade's or the
-- sheet's terms. Null: none of their own.
ALTER TABLE person ADD COLUMN terms INT;
