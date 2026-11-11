-- A sheet's fiscal year start month, 1 to 12, not dated: a fiscal year runs twelve months from its
-- first day in that month, and is named FY<yyyy> by the calendar year that day falls in. Null: 1.
ALTER TABLE sheet ADD COLUMN fiscal_start_month INT;
