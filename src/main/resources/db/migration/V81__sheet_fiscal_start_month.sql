-- The sheet's fiscal year start month (1 to 12; not dated). A fiscal year runs twelve months from the first day of
-- that month, and is named FY<yyyy> by the calendar year its first day falls in.
ALTER TABLE sheet ADD COLUMN fiscal_start_month INT NOT NULL DEFAULT 1;
