-- The sheet's today a steward waived a claim's late charge on (none: not waived); once only.
ALTER TABLE claim ADD COLUMN waived_on DATE;
