-- Whether the claim is held: a held OPEN or PENDING claim leaves the live totals as a hold on its person would.
ALTER TABLE claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
