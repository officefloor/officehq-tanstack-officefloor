-- A sheet's pot (two decimals; null means none; not dated): each member of a balanced pool gets a cut of it.
ALTER TABLE sheet ADD COLUMN pot DECIMAL(12, 2);
