-- A sheet's entry date (null means off): while it is set, it is the default date of a new claim (the
-- claim form, the quick-add bar, a pasted line with a blank date and a correction) instead of the
-- sheet's today. Not dated.
ALTER TABLE sheet ADD COLUMN entry_date DATE;
