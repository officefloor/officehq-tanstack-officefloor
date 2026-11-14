-- How many proofs a claim has: raised by one at a time, while it is neither SETTLED nor VOID. A claim
-- whose due is above the proof threshold in force on its date cannot be settled or approved at 0.
ALTER TABLE claim ADD COLUMN proofs INT NOT NULL DEFAULT 0;
