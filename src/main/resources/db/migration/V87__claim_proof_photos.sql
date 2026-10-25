-- How many of a claim's proofs are photos: the rest are receipts (every proof added before kinds is a receipt).
ALTER TABLE claim ADD COLUMN proofs_photo INT DEFAULT 0 NOT NULL;
