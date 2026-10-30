-- A period can be closed: closed_total is its due-total fixed at that moment, kept whatever the
-- rates do later. No claim dated inside a closed period can be added, edited or change status.
ALTER TABLE period ADD COLUMN closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(14,2);
