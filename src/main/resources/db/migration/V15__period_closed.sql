-- A period can be closed. Closing fixes its due-total at that moment (later rate changes do not
-- alter it), and no claim dated inside a closed period can be added, edited, opened, settled or voided.
ALTER TABLE period ADD COLUMN closed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(19, 2);
