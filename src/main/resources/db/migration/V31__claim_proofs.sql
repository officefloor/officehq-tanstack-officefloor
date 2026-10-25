-- How many proofs a claim carries: raised by one at a time while it is neither SETTLED nor VOID. A claim whose due is
-- above the proof threshold in force on its date cannot be settled or approved while it has none.
ALTER TABLE claim ADD COLUMN proofs INT DEFAULT 0 NOT NULL;
