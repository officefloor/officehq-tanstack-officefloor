-- Whether a steward waived the claim's late charge: a waived claim bears no late charge from then on, overdue or not.
-- Only an OPEN or PENDING claim can be waived, and only once.
ALTER TABLE claim ADD COLUMN waived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE claim_history ADD COLUMN waived BOOLEAN NOT NULL DEFAULT FALSE;
