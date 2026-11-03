-- A sheet's maximum of open claims per person (null means none; not dated): opening a claim is refused while its
-- person already has that many OPEN or PENDING claims.
ALTER TABLE sheet ADD COLUMN max_open INT;
