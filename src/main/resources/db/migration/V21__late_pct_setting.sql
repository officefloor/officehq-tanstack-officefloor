-- A sheet's late percent: what an overdue claim's due gains, per full 30 days overdue, with two decimals.
ALTER TABLE sheet ADD COLUMN late_pct DECIMAL(19, 2) DEFAULT 0.00 NOT NULL;
