-- Whether a period is soft-closed: its due-total (closed_total) and each person's figure for it
-- (period_person_total) are fixed, but claims dated inside it can still be changed. A claim added
-- while the period its date falls in is soft-closed is a late entry.
ALTER TABLE period ADD COLUMN soft_closed BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN DEFAULT FALSE NOT NULL;
