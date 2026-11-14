-- The sheet's escalation gap in days: reminding a claim whose previous reminder was at least this many days before the
-- sheet's today escalates it; null: off. Not dated.
ALTER TABLE sheet ADD COLUMN escalate_after INT;
-- Whether the claim was ever escalated (a claim is escalated once at most).
ALTER TABLE claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
