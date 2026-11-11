-- The sheet's today when the share was added (null: before shares were dated, held always): a sheet
-- read as of a date shows only the shares added on or before it.
ALTER TABLE claim_share ADD COLUMN on_date DATE;
