-- Whether the claim is held: while OPEN or PENDING it counts toward no total, unless a statement binds it.
ALTER TABLE claim ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE claim_history ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
