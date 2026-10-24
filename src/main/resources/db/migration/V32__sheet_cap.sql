-- The sheet's cap: a person's due-total is at most it. Two decimals; null (blank) when there is none.
ALTER TABLE sheet ADD COLUMN cap DECIMAL(19, 2);
