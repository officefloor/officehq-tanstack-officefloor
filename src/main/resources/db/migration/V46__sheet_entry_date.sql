-- The sheet's entry date: while it is set, it is the default date of a new claim wherever the sheet's today was
-- (the claim form, the quick-add bar, a pasted line with a blank date, a correction). Null (blank) means off.
ALTER TABLE sheet ADD COLUMN entry_date DATE;
