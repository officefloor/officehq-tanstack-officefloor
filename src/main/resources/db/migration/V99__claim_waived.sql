-- Whether a steward waived the claim's late charge: it bears none from then on.
ALTER TABLE claim ADD COLUMN waived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE claim_history ADD COLUMN waived BOOLEAN NOT NULL DEFAULT FALSE;
