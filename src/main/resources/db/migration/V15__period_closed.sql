-- A closed period: its due-total is fixed at the moment it was closed (closed_total), and no claim
-- dated inside it can be added, edited, opened, settled or voided. Open until closed.
ALTER TABLE period ADD COLUMN closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(19, 2);
ALTER TABLE period ADD COLUMN closed_by VARCHAR(200);
ALTER TABLE period ADD COLUMN closed_at TIMESTAMP;
