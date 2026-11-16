-- A claim's proofs count: 0 when added, raised by one at a time (not once SETTLED or VOID, nor while dated inside a
-- closed period). A claim whose due is above the sheet's proof threshold cannot be settled nor approved while it is 0.
ALTER TABLE claim ADD COLUMN proofs INT DEFAULT 0 NOT NULL;
