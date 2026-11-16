-- The most OPEN or PENDING claims a person may have in the period (those whose due-by falls in it), or null for none.
ALTER TABLE period ADD COLUMN cap INT;
