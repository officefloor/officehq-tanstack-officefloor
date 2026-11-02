-- A claim's photo proofs: of its proofs count, how many are photos (the rest receipts).
ALTER TABLE claim ADD COLUMN proof_photos INT NOT NULL DEFAULT 0;
