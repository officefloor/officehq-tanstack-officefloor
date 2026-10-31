-- The sheet's grace days: when the sheet's today is changed, every open period whose to-date plus
-- this many days is before the new today is closed. Null (or no row) is off.
ALTER TABLE sheet_setting ADD COLUMN grace_days INT;
