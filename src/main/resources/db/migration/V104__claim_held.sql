-- Whether the claim is held: only an OPEN or PENDING claim is held or released. A held OPEN claim not bound by a
-- statement is left out of every live total, as a held person's claims are.
ALTER TABLE claim ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE claim_history ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
