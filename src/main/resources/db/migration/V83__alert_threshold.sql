-- A sheet's alert threshold, with two decimals, not dated: a person whose due-total goes from at or
-- below it (or none) to above it is alerted. Null: off.
ALTER TABLE sheet ADD COLUMN alert_threshold DECIMAL(19, 2);
