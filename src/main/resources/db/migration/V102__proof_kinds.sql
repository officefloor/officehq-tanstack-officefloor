-- How many of a claim's proofs are photos (the rest are receipts), and the sheet's proof kinds, a
-- comma separated list of kinds (null: none): a claim above the proof threshold needs a proof of each.
ALTER TABLE claim ADD COLUMN proof_photos INT DEFAULT 0 NOT NULL;
ALTER TABLE claim_history ADD COLUMN proof_photos INT DEFAULT 0 NOT NULL;
ALTER TABLE sheet ADD COLUMN proof_kinds VARCHAR(100);
