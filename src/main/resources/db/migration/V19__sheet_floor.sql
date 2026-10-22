-- The sheet's floor (two decimals): a claim's live due is at least it. A settled claim's fixed due, a figure a
-- statement bound and a closed period's fixed total keep the figure they took.
ALTER TABLE sheet ADD COLUMN due_floor DECIMAL(14, 2) NOT NULL DEFAULT 1.00;
