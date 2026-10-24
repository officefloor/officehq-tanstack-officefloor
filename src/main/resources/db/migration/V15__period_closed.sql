-- A period can be closed: its due-total is then fixed at what it was on closing, and no claim dated in it can
-- be added, edited or change status.
ALTER TABLE period ADD COLUMN closed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(19, 2);
