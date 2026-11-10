-- A sheet's cap: the most a person's due-total may be, with two decimals. Null: no cap.
ALTER TABLE sheet ADD COLUMN cap DECIMAL(19, 2);
