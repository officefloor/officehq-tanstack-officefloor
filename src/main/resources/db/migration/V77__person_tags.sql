-- A person may carry tags (null: none): trimmed, lower-cased, without repeats, sorted and joined by commas.
-- Each is kept among the tags ever used on the sheet (claim_tag), as a claim's are.
ALTER TABLE person ADD COLUMN tags VARCHAR(1000);
