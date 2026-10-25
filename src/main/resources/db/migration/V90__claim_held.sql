-- Whether the claim is held: while held, an OPEN or PENDING claim counts in no live total, as a held person's claims
-- do, unless a statement binds it.
ALTER TABLE claim ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
