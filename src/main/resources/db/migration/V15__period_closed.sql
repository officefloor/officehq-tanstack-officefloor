-- Whether a period is closed, and the due-total fixed when it was closed (null while open). No claim
-- dated inside a closed period can be added, edited, opened, settled or voided.
ALTER TABLE period ADD COLUMN closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(19, 2);
