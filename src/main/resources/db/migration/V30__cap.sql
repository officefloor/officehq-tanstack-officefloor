-- The sheet's cap: a person's due-total is at most it. NULL: no cap.
ALTER TABLE sheet ADD COLUMN due_cap DECIMAL(12,2);
