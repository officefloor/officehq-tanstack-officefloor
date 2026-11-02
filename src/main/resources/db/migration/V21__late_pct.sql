-- A sheet's late percent: an overdue claim's due gains it (of its figure) for every full 30 days
-- it is overdue (two decimals). It starts at 0.00.
ALTER TABLE sheet ADD COLUMN late_pct DECIMAL(9, 2) NOT NULL DEFAULT 0.00;
