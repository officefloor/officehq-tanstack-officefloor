-- A claim's proofs count: raised by one at a time; a claim whose due is above the sheet's proof threshold needs one
-- before it is settled or approved.
ALTER TABLE claim ADD COLUMN proofs INT NOT NULL DEFAULT 0;
