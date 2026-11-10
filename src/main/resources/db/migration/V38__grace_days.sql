-- A sheet's grace days: a whole number of days, not dated. Null: off. When the sheet's today is
-- changed, every open period whose to-date plus the grace days is before the new today is closed.
ALTER TABLE sheet ADD COLUMN grace_days INT;
