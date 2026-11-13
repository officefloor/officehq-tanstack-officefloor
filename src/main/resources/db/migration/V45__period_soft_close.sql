-- A period can be soft-closed: its due-total (closed_total) and each person's figure for it (period_person_total)
-- are fixed then, as for a close, but claims dated inside it can still be added, edited, opened, settled and voided.
ALTER TABLE period ADD COLUMN soft_closed BOOLEAN DEFAULT FALSE NOT NULL;
-- A claim added while the period containing its date was soft-closed is a late entry; it stays one.
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN DEFAULT FALSE NOT NULL;
