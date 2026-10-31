-- The sheet's maximum of OPEN or PENDING claims per person (not dated). Null: none.
ALTER TABLE sheet ADD COLUMN max_open INT;
