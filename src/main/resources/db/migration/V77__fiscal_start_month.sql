-- A sheet's fiscal year start month (1 to 12; not dated): a fiscal year runs twelve months from the first day of
-- that month, named FY<yyyy> by the calendar year its first day falls in.
ALTER TABLE sheet ADD COLUMN fiscal_start_month INT DEFAULT 1 NOT NULL CHECK (fiscal_start_month BETWEEN 1 AND 12);
