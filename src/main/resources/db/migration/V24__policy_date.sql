-- The sheet's policy date: a claim dated before it rounds half up, one dated on or after it half
-- to even. Null (or no row) is the sheet's today.
ALTER TABLE sheet_setting ADD COLUMN policy_date DATE;
