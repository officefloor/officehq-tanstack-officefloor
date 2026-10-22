-- The sheet's late percent (two decimals): an overdue claim's due gains it, of its figure, for every full 30 days
-- it is overdue. A settled claim's fixed due keeps the charge it had then.
ALTER TABLE sheet ADD COLUMN late_pct DECIMAL(7, 2) NOT NULL DEFAULT 0.00;
