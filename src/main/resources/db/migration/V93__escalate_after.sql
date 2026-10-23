-- The sheet's escalation gap (days; null: off; not dated). Reminding a claim whose previous reminder was at least
-- that many days before the sheet's today escalates it, once per claim ever (escalated).
ALTER TABLE sheet ADD COLUMN escalate_after INT;
ALTER TABLE claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
