-- The sheet's fiscal year start month, 1 to 12: a fiscal year runs from its first day for twelve months. Not dated.
ALTER TABLE sheet ADD COLUMN fiscal_start_month INT DEFAULT 1 NOT NULL;
