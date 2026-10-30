-- A claim's proofs count: raised by one at a time. A claim whose due is above the proof threshold in force on its
-- date (a dated setting, proof-threshold; none when blank) cannot be settled or approved while it is 0.
ALTER TABLE claim ADD COLUMN proofs INT NOT NULL DEFAULT 0;
