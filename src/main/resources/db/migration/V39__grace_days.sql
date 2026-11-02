-- A sheet's grace days (null means off; not dated): when the sheet's today is changed, every open period
-- whose to-date plus the grace days is before the new today is closed.
ALTER TABLE sheet ADD COLUMN grace_days INT;
