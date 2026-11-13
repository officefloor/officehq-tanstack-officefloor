-- The number of the claim a claim was split from (null: not split off another claim). The claim split keeps
-- none; only the new claim carries it.
ALTER TABLE claim ADD COLUMN split_from VARCHAR(120);
