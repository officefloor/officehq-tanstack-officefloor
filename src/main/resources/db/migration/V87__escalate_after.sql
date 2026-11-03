-- A sheet's escalation gap (null means off; not dated): reminding a claim whose previous reminder was at least that
-- many days before the sheet's today escalates it, once per claim ever.
ALTER TABLE sheet ADD COLUMN escalate_after INT;
ALTER TABLE claim ADD COLUMN escalated BOOLEAN NOT NULL DEFAULT FALSE;
