-- A claim may be a correction: a negative amount that names, by number, a SETTLED claim of the same person it
-- corrects (null: not a correction). The amount may now be below 0.00 for a correction; the column is rebuilt to
-- drop its 0.00-or-more check.
ALTER TABLE claim ADD COLUMN corrects INT;
ALTER TABLE claim ADD COLUMN amount_signed DECIMAL(14, 2);
UPDATE claim SET amount_signed = amount;
ALTER TABLE claim DROP COLUMN amount;
ALTER TABLE claim ALTER COLUMN amount_signed RENAME TO amount;
ALTER TABLE claim ALTER COLUMN amount SET NOT NULL;
