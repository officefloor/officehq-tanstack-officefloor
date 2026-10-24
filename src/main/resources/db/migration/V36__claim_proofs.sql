-- A claim's proofs count: raised by one at a time (not on a SETTLED or VOID claim); 0 for every claim to begin with.
ALTER TABLE claim ADD COLUMN proofs INT DEFAULT 0 NOT NULL;
