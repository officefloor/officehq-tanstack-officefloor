-- A soft-closed period: its due-total (closed_total) and each person's (period_person_total) are fixed as when it is
-- closed, but claims dated inside it may still be added, edited and change status. Never both soft and closed;
-- closing or reopening it clears soft.
ALTER TABLE period ADD COLUMN soft BOOLEAN DEFAULT FALSE NOT NULL;
-- A claim added while the period its date falls in was soft-closed.
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN DEFAULT FALSE NOT NULL;
