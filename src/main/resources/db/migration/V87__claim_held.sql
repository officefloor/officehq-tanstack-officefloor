-- An OPEN or PENDING claim may be held and released: a held claim counts in no live total unless a statement binds it,
-- as a held person's claims.
ALTER TABLE claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE hist.claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE asof.claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
