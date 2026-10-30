-- The sheet's grace days: when its today is changed, every open period whose to-date plus them is
-- before the new today is closed. NULL: off. Not dated.
ALTER TABLE sheet ADD COLUMN grace_days INT;
