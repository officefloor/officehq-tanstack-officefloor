-- A claim's tags, lower-cased, sorted and comma separated (empty for none), and every tag it has ever carried
-- (likewise), so the sheet's tags stay listed after an edit drops one.
ALTER TABLE claim ADD COLUMN tags VARCHAR(4000) DEFAULT '' NOT NULL;
ALTER TABLE claim ADD COLUMN tags_ever VARCHAR(4000) DEFAULT '' NOT NULL;
