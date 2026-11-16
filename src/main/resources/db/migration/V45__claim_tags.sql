-- A claim's tags (null when it has none): each trimmed and in lower case, no repeats, sorted, joined by commas.
-- claim_edit keeps the tags a claim had before an edit, so every tag ever used on a sheet is in one or the other.
ALTER TABLE claim ADD COLUMN tags VARCHAR(2000);
ALTER TABLE claim_edit ADD COLUMN tags VARCHAR(2000);
