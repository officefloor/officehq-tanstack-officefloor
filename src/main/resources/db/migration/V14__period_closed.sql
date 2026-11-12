-- A closed period: its due-total is fixed at closed_total (the figures of the claims that counted in
-- it when it was closed, skipping n/a); no claim dated inside it may be added, edited or moved on.
ALTER TABLE period ADD COLUMN closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(19, 2);
