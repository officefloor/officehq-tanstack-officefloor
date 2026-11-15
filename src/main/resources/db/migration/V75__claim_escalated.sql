-- Whether a claim was ever escalated by a reminder (see the escalate-after setting); once only.
ALTER TABLE claim ADD COLUMN escalated BOOLEAN NOT NULL DEFAULT FALSE;
