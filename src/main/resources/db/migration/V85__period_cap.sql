-- A period's cap: the most OPEN or PENDING claims a person may have due by a date in it (NULL: none).
ALTER TABLE period ADD COLUMN cap INT;
