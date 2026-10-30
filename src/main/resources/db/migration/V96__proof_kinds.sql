-- A proof has a kind, receipt or photo: the claim keeps how many of its proofs are photos (the rest are receipts).
ALTER TABLE claim ADD COLUMN proof_photos INT DEFAULT 0 NOT NULL;
ALTER TABLE claim_transferred ADD COLUMN proof_photos INT DEFAULT 0 NOT NULL BEFORE transferred_on;
ALTER TABLE claim_history ADD COLUMN proof_photos INT;

-- The sheet's proof kinds (not dated): a comma separated list of kinds; while set, a claim above the proof threshold
-- needs at least one proof of each listed kind before it can be settled or approved. Null: none.
ALTER TABLE sheet ADD COLUMN proof_kinds VARCHAR(200);
