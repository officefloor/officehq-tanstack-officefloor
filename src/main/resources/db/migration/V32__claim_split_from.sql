-- A claim split off another names, by number, the claim it was split from; null for any other claim.
ALTER TABLE claim ADD COLUMN split_from VARCHAR(64);
