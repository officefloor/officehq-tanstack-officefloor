-- The number of the claim a claim was split off: set when an OPEN claim is split, on the new claim only.
ALTER TABLE claim ADD COLUMN split_from VARCHAR(255);
