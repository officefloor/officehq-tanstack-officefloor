-- The sheet's maximum of OPEN and PENDING claims per person: opening a claim of a person who already has this many is
-- refused; null: none. Not dated.
ALTER TABLE sheet ADD COLUMN max_open INT;
