-- A period can be soft-closed: its due-total (closed_total) and each person's (period_person_total) are fixed as for a
-- close, but claims dated inside it can still be added, edited, opened, settled and voided. A soft-closed period is
-- not closed; closing it fixes the figures afresh, reopening drops them.
ALTER TABLE period ADD COLUMN soft_closed BOOLEAN DEFAULT FALSE NOT NULL;
-- Whether a claim was added while the period its date falls in was soft-closed.
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN DEFAULT FALSE NOT NULL;
