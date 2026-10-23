-- The most OPEN or PENDING claims a person may have in the period at once (a whole number of at least 1), or NULL for
-- no cap. Opening a claim belonging to the period is refused when its person already has that many in it.
ALTER TABLE period ADD COLUMN open_cap INT;
