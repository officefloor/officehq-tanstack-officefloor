-- The sheet's escalation gap in days (not dated). Null: off.
ALTER TABLE sheet ADD COLUMN escalate_after INT;
-- Whether the claim has been escalated: a claim is escalated once ever.
ALTER TABLE claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
