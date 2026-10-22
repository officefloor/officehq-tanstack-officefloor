-- The sheet's entry date (null: off; not dated). While it is set, a new claim given no date (the claim form, the
-- quick-add bar, a pasted line with a blank date, a correction) is dated it instead of the sheet's today.
ALTER TABLE sheet ADD COLUMN entry_date DATE;
