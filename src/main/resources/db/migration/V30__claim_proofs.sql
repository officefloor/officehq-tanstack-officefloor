-- How many proofs a claim has been given, raised by one at a time; 0 for a claim given none.
ALTER TABLE claim ADD COLUMN proofs INT NOT NULL DEFAULT 0;
