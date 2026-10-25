-- A period can be soft-closed: its due-total (closed_total) and each person's (period_person_total) are fixed as
-- closing fixes them, but claims dated inside it can still be added, edited, opened, settled and voided. A
-- soft-closed period is not closed; closing it fixes the figures afresh, reopening removes them.
ALTER TABLE period ADD COLUMN soft_closed BOOLEAN DEFAULT FALSE NOT NULL;
-- A claim added while the period containing its date was soft-closed is a late entry, for good.
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN DEFAULT FALSE NOT NULL;
