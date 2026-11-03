-- A proof has a kind, receipt or photo (receipt when none is chosen): a claim keeps how many of its proofs are photos
-- (the rest of its proofs are receipts). A sheet's proof kinds (a comma separated list of kinds; null means none; not
-- dated): while set, a claim above the proof threshold needs at least one proof of each listed kind to be settled or approved.
ALTER TABLE claim ADD COLUMN proofs_photo INT DEFAULT 0 NOT NULL;
ALTER TABLE sheet ADD COLUMN proof_kinds VARCHAR(100);
