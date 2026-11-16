-- A claim is escalated at most once ever: when it is reminded while the sheet's escalation gap is set and its previous
-- reminder was at least that many days before the sheet's today. True once escalated.
ALTER TABLE claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
