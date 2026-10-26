-- A period can be closed. Closing fixes its due-total at that moment: closed_total is that figure
-- (null while the period is open), and later changes to rates do not alter it.
ALTER TABLE period ADD COLUMN closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(15, 2);
