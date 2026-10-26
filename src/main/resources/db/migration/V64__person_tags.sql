-- A person may carry tags (null: none): trimmed, lower-cased, without repeats, sorted and joined by commas.
-- Each is noted in claim_tag as used on the sheet, so the tag totals list it.
ALTER TABLE person ADD COLUMN tags VARCHAR(1000);
-- The sheet's snapshots, and their layout as of a date, carry it too.
ALTER TABLE hist.person ADD COLUMN tags VARCHAR(1000);
ALTER TABLE asof.person ADD COLUMN tags VARCHAR(1000);
