-- The sheet's grace days (null: off; not dated). When the sheet's today is changed, each open period whose to-date
-- plus them is before the new today is closed.
ALTER TABLE sheet ADD COLUMN grace_days INT;
