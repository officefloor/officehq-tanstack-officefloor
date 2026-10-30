-- Whether a steward has waived the claim's late charge: a waived claim bears no late charge from then on,
-- whatever its overdue state. Waived once ever.
ALTER TABLE claim ADD COLUMN waived BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim_transferred ADD COLUMN waived BOOLEAN DEFAULT FALSE NOT NULL BEFORE transferred_on;
ALTER TABLE claim_history ADD COLUMN waived BOOLEAN;
