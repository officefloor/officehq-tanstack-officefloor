-- How many proofs a claim has been given (raised by one at a time; 0 for a claim given none). A claim whose
-- due is above the sheet's proof threshold (a dated setting, key proof-threshold) in force on its date cannot
-- be settled or approved while it has none.
ALTER TABLE claim ADD COLUMN proofs INT DEFAULT 0 NOT NULL;
