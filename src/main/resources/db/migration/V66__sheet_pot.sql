-- The sheet's pot (two decimals; null: none; not dated). Each member of a balanced pool is shown a cut of it.
ALTER TABLE sheet ADD COLUMN pot DECIMAL(14, 2);
