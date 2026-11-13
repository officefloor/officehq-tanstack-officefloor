-- The sheet's pot: shared out among the members of each balanced pool by their shares; null: none. Not dated.
ALTER TABLE sheet ADD COLUMN pot DECIMAL(15, 2);
