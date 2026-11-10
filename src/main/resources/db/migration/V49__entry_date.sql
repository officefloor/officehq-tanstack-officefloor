-- A sheet's entry date (null: off): while set, the default date of a new claim instead of the sheet's today.
ALTER TABLE sheet ADD COLUMN entry_date DATE;
