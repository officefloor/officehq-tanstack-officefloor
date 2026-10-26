-- A correction (a claim with a negative amount) names, by number, the SETTLED claim of the same person it
-- corrects; null for any other claim. The amount's old "0 or more" check (unnamed) goes with its column, which is
-- rebuilt so that only a correction may be negative.
ALTER TABLE claim ADD COLUMN corrects BIGINT;
ALTER TABLE claim ADD COLUMN amount_signed DECIMAL(15, 2);
UPDATE claim SET amount_signed = amount;
ALTER TABLE claim DROP COLUMN amount;
ALTER TABLE claim ALTER COLUMN amount_signed RENAME TO amount;
ALTER TABLE claim ALTER COLUMN amount SET NOT NULL;
ALTER TABLE claim ADD CONSTRAINT claim_amount_correction CHECK (amount >= 0 OR corrects IS NOT NULL);
