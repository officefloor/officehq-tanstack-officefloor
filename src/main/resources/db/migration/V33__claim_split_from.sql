-- The number of the claim a claim was split from (none for any other claim, and for the original
-- of a split).
ALTER TABLE claim ADD COLUMN split_from VARCHAR(220);
