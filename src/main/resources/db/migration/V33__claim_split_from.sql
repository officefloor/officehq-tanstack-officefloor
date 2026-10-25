-- The number of the claim an OPEN claim was split off (null: not split off one). The claim split keeps the rest of its
-- amount and shows nothing.
ALTER TABLE claim ADD COLUMN split_from VARCHAR(50);
