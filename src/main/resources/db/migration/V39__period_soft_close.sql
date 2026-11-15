-- A soft-closed period: its due-total (closed_total) and each person's figure for it
-- (period_person_total) are fixed at the moment it was soft-closed, but claims dated inside it can
-- still be added and changed. It can then be closed (fixing the figures afresh) or reopened.
ALTER TABLE period ADD COLUMN soft_closed BOOLEAN DEFAULT FALSE NOT NULL;
-- A claim added while the period its date falls in was soft-closed.
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN DEFAULT FALSE NOT NULL;
