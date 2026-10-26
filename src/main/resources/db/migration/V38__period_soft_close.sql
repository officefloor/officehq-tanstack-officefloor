-- A period can be soft-closed: its due-total (closed_total) and its people's figures (period_person_total) are fixed,
-- but claims dated in it can still change. A claim added while its period is soft-closed is a late entry.
ALTER TABLE period ADD COLUMN soft BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN DEFAULT FALSE NOT NULL;
