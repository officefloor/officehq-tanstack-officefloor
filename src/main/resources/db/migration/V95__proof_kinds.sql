-- The sheet's proof kinds, comma separated (receipt, photo): while set, a claim whose due is above the proof threshold
-- needs at least one proof of each listed kind before it can be settled or approved; null: none. Not dated.
ALTER TABLE sheet ADD COLUMN proof_kinds VARCHAR(100);
-- How many of a claim's proofs are photos; the rest of its proofs are receipts (a proof added with no kind chosen is one).
ALTER TABLE claim ADD COLUMN photo_proofs INT NOT NULL DEFAULT 0;
