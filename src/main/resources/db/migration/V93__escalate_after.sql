-- A sheet's escalation gap in days, not dated (null: off): reminding a claim whose previous reminder
-- was at least that many days before the sheet's today escalates it, once per claim ever.
ALTER TABLE sheet ADD COLUMN escalate_after INT;
-- Whether a claim has been escalated; it is escalated at most once.
ALTER TABLE claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
