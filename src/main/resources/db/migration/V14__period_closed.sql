-- A period can be closed: closing fixes its due-total at that moment (`closed_total`, which later
-- rate changes do not alter), and no claim dated in it can then be added, edited or change status.
ALTER TABLE period ADD COLUMN closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period ADD COLUMN closed_total DECIMAL(14,2);
