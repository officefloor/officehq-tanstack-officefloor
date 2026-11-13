-- A period can be closed: its due-total is then fixed at closed_total, whatever the rates do later,
-- and no claim dated inside it can be added, edited, opened, settled or voided.
ALTER TABLE period ADD COLUMN closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(19, 2);
