-- The sheet's floor: a claim's live due is at least it. Two decimals; a sheet starts at 1.00.
ALTER TABLE sheet ADD COLUMN floor DECIMAL(19, 2) DEFAULT 1.00 NOT NULL;
