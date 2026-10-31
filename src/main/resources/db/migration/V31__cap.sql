-- The sheet's cap, two decimals: a person's due-total is at most it. Null (or no row) is no cap.
ALTER TABLE sheet_setting ADD COLUMN cap DECIMAL(12,2);
