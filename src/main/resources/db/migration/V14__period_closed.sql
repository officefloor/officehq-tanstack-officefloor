-- A period may be closed. Closing fixes its due-total at that moment (the due of its claims that
-- count, at the rates then in force); later rate changes do not alter it. None while open. No claim
-- dated inside a closed period can be added, edited, opened, settled or voided.
ALTER TABLE period ADD COLUMN closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(14, 2);
