-- The sheet's cap (two decimals; NULL: none). A person's due-total is at most it; the rest is their excess.
ALTER TABLE sheet ADD COLUMN due_cap DECIMAL(14, 2);
