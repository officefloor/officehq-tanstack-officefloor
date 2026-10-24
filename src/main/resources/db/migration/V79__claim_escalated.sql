-- Whether a reminder ever escalated the claim (its previous reminder at least the sheet's escalation gap before).
ALTER TABLE claim ADD COLUMN escalated BOOLEAN NOT NULL DEFAULT FALSE;
