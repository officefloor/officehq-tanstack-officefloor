-- For a claim split off another OPEN claim, the number of the claim it was split from. Null: not split off one.
ALTER TABLE claim ADD COLUMN split_from VARCHAR(250);
