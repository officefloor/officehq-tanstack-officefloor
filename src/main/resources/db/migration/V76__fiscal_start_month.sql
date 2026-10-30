-- The sheet's fiscal year start month, 1 to 12. NULL: 1 (January). Not dated.
ALTER TABLE sheet ADD COLUMN fiscal_start_month INT;
