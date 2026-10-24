-- A claim's tags: trimmed, lower-cased, without repeats, sorted and comma separated; null for a claim with none.
ALTER TABLE claim ADD COLUMN tags VARCHAR(1024);
-- Every tag the claim has ever carried, sorted and comma separated; null while it never had any.
ALTER TABLE claim ADD COLUMN tags_used VARCHAR(1024);
