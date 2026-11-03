-- Whether an OPEN or PENDING claim is held: a held claim counts in no live total, unless a statement binds it.
ALTER TABLE claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim_history ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
