-- A soft-closed period: its due-total and each person's are fixed (as on closing), but claims dated in it can
-- still be added, edited and change status. A claim added while its period is soft-closed is a late entry.
ALTER TABLE period ADD COLUMN soft_closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN DEFAULT FALSE NOT NULL;
