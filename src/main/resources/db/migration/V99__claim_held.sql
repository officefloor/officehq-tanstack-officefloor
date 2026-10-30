-- Whether the claim is held: an OPEN or PENDING claim held, unless a statement binds it, counts in no live total
-- (as a held person's claims). Held and released with a toggle.
ALTER TABLE claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim_transferred ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL BEFORE transferred_on;
ALTER TABLE claim_history ADD COLUMN held BOOLEAN;
