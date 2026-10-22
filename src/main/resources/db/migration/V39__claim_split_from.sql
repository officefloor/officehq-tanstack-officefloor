-- The number of the claim an OPEN claim was split from (null: not split off one). Splitting an OPEN claim leaves
-- the original with the rest of its amount (and no split-from of its own) and adds a new OPEN claim with the
-- split-off amount, the same date and note and the next number.
ALTER TABLE claim ADD COLUMN split_from VARCHAR(220);
