-- The sheet's late percent: an overdue claim's due gains this percent of its figure for every full 30 days it is overdue.
ALTER TABLE sheet ADD COLUMN late_pct DECIMAL(15, 2) NOT NULL DEFAULT 0.00;
