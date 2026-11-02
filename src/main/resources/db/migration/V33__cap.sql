-- A sheet's cap: a person's due-total is at most it (two decimals; null means no cap).
ALTER TABLE sheet ADD COLUMN cap DECIMAL(12, 2);
