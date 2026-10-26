-- A claim is escalated at most once ever: when it is reminded at least the sheet's escalation gap after its previous reminder.
ALTER TABLE claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE hist.claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE asof.claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
