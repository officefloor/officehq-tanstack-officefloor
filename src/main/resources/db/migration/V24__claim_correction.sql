-- A claim with a negative amount is a correction: it names, by number, the SETTLED claim of the
-- same person it corrects. The amount loses its 0.00-or-more check (rebuilt without it), and a
-- claim keeps the number it corrects (null for an ordinary claim).
ALTER TABLE claim ADD COLUMN amount_signed DECIMAL(14, 2);
UPDATE claim SET amount_signed = amount;
ALTER TABLE claim DROP COLUMN amount;
ALTER TABLE claim ALTER COLUMN amount_signed RENAME TO amount;
ALTER TABLE claim ALTER COLUMN amount SET NOT NULL;
ALTER TABLE claim ADD COLUMN corrects BIGINT;
