-- The number of the claim a claim was split from: set on the new claim a split makes. Null for any other claim.
ALTER TABLE claim ADD COLUMN split_from VARCHAR(64);
