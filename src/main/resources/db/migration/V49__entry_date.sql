-- The sheet's entry date: while it is set, it is the date a new claim left without one takes (the claim form, the
-- quick-add bar, a pasted line, a correction) instead of the sheet's today; null: off.
ALTER TABLE sheet ADD COLUMN entry_date DATE;
