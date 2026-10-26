-- A proof has a kind, receipt or photo: of a claim's proofs, how many are photos (the rest are receipts).
ALTER TABLE claim ADD COLUMN proofs_photo INT DEFAULT 0 NOT NULL;
ALTER TABLE hist.claim ADD COLUMN proofs_photo INT DEFAULT 0 NOT NULL;
ALTER TABLE asof.claim ADD COLUMN proofs_photo INT DEFAULT 0 NOT NULL;
