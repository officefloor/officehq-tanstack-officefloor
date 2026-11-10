-- A claim's number: given when it is opened, the next on its sheet (1, 2, ...), with no gaps and
-- never reused. A draft has none; voiding a claim clears it. The sheet keeps the last number given.
ALTER TABLE sheet ADD COLUMN last_claim_number INT NOT NULL DEFAULT 0;
ALTER TABLE claim ADD COLUMN claim_number INT;
