-- The sheet's maximum of OPEN or PENDING claims per person (a whole number; null: none; not dated). Opening a claim
-- (alone, by the bulk button or a paste) is refused while its person already has that many.
ALTER TABLE sheet ADD COLUMN max_open INT;
