-- Each proof has a kind, receipt or photo (receipt when none is chosen): a claim keeps how many of its proofs are photos
-- (the rest of its proofs count are receipts). The sheet's proof kinds (setting_value key proof-kinds, a comma separated
-- list, not dated; blank: none): while set, a claim above the proof threshold needs at least one proof of each listed
-- kind before it can be settled or approved.
ALTER TABLE claim ADD COLUMN proof_photos INT NOT NULL DEFAULT 0;
ALTER TABLE claim_history ADD COLUMN proof_photos INT NOT NULL DEFAULT 0;
ALTER TABLE sheet ADD COLUMN proof_kinds VARCHAR(100);
