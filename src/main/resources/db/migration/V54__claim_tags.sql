-- A claim's tags: trimmed, lower-cased, without repeats, sorted and comma separated; null when it has none.
-- Kept in its history too, so every tag ever used on a sheet can be read back from it.
ALTER TABLE claim ADD COLUMN tags VARCHAR(2000);
ALTER TABLE claim_history ADD COLUMN tags VARCHAR(2000);
