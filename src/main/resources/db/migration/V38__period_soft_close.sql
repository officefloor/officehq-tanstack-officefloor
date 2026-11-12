-- A soft-closed period: its due-total (closed_total) and each person's due-total in it
-- (period_person_total) are fixed as when it was soft-closed, but claims dated inside it may still be
-- added, edited and moved on. It is not closed; closing it fixes the figures afresh, reopening drops them.
ALTER TABLE period ADD COLUMN soft BOOLEAN DEFAULT FALSE NOT NULL;
-- A claim added while the period its date falls in was soft-closed.
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN DEFAULT FALSE NOT NULL;
