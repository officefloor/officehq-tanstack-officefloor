-- The number of the claim a claim was split off; null when it was not split off one.
ALTER TABLE claim ADD COLUMN split_from VARCHAR(200);
