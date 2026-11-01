-- A closed period: closed_total is its due-total fixed when it was closed (later rate changes do not move it).
-- No claim dated inside a closed period may be added, edited or change status.
ALTER TABLE period ADD COLUMN closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(15, 2);
