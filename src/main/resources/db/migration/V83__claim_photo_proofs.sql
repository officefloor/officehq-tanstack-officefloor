-- How many of a claim's proofs are photos: the rest of its proofs count are receipts (a proof added with
-- no kind chosen is a receipt).
ALTER TABLE claim ADD COLUMN photo_proofs INT NOT NULL DEFAULT 0;
