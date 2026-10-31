-- A correction is a claim with a negative amount: it names, by number, the SETTLED claim of the
-- same person it corrects. `corrects` is that number; null for any other claim.
ALTER TABLE claim ADD COLUMN corrects BIGINT;
-- `amount` may now be negative (a correction): the column is replaced to drop its `>= 0` check.
ALTER TABLE claim ADD COLUMN signed_amount DECIMAL(14,2);
UPDATE claim SET signed_amount = amount;
ALTER TABLE claim DROP COLUMN amount;
ALTER TABLE claim ALTER COLUMN signed_amount RENAME TO amount;
ALTER TABLE claim ALTER COLUMN amount SET NOT NULL;
