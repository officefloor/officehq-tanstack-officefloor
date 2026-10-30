-- A period can be soft-closed: its due-total (closed_total) and each person's figure for it are fixed, but
-- claims dated inside it can still be added and changed. A claim added while its period is soft-closed is a
-- late entry.
ALTER TABLE period ADD COLUMN soft_closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN DEFAULT FALSE NOT NULL;
