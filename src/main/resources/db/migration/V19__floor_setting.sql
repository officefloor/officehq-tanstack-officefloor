-- A sheet's floor: the least a claim's live due may be, with two decimals.
ALTER TABLE sheet ADD COLUMN due_floor DECIMAL(19, 2) DEFAULT 1.00 NOT NULL;
