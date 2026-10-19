-- A closed period keeps the due-total it had when it was closed (closed_total), whatever rates do
-- later. No claim dated inside a closed period may be added, edited, opened, settled or voided.
ALTER TABLE period ADD COLUMN closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(12, 2);
