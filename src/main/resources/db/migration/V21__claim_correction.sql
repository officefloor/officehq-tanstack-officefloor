-- A claim with a negative amount is a correction of a SETTLED claim of the same person: it keeps
-- the number of the claim it corrects (none for any other claim). The amount may now be negative:
-- the column is rebuilt without its old 0-or-more check.
ALTER TABLE claim ADD COLUMN signed_amount DECIMAL(19, 2);
UPDATE claim SET signed_amount = amount;
ALTER TABLE claim DROP COLUMN amount;
ALTER TABLE claim ALTER COLUMN signed_amount RENAME TO amount;
ALTER TABLE claim ALTER COLUMN amount SET NOT NULL;
ALTER TABLE claim ADD COLUMN corrects BIGINT;
