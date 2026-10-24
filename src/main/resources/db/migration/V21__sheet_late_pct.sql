-- The sheet's late percent: an overdue claim's due gains this percent of its figure for every full 30 days it is
-- overdue. Two decimals; a sheet starts at 0.00.
ALTER TABLE sheet ADD COLUMN late_pct DECIMAL(19, 2) DEFAULT 0.00 NOT NULL;
