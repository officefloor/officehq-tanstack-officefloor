-- The sheet's floor: a claim's live due is at least it. Null (or no row) is the default, 1.00.
ALTER TABLE sheet_setting ADD COLUMN floor DECIMAL(12,2);
