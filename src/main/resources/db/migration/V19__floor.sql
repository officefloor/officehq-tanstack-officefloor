-- A sheet's floor: a claim's live due is at least it (two decimals). It starts at 1.00.
ALTER TABLE sheet ADD COLUMN floor DECIMAL(12, 2) NOT NULL DEFAULT 1.00;
