-- The sheet's cap: a person's due-total is at most it; null: no cap.
ALTER TABLE sheet ADD COLUMN due_cap DECIMAL(15, 2);
