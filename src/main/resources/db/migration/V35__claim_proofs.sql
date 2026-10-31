-- How many proofs a claim has, raised by one at a time (not on a SETTLED or VOID claim, nor one
-- dated in a closed period). A claim whose due is above the sheet's proof threshold in force on its
-- date cannot be settled or approved while it has none.
ALTER TABLE claim ADD COLUMN proofs INT NOT NULL DEFAULT 0;
