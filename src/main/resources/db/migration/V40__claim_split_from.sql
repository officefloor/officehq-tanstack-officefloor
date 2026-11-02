-- For a claim split off another, the number of the claim it was split from (null otherwise).
ALTER TABLE claim ADD COLUMN split_from VARCHAR(200);
