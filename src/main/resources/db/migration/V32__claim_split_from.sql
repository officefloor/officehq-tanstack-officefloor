-- The number (as it shows) of the claim a claim was split from (null unless it was split off one).
ALTER TABLE claim ADD COLUMN split_from VARCHAR(220);
