-- A claim's number: given when it is opened, the sheet's next one (1, 2, ...), never reused.
-- A draft has none, and voiding a claim clears it. The sheet keeps the last number it gave.
ALTER TABLE sheet ADD COLUMN last_claim_number INT NOT NULL DEFAULT 0;
ALTER TABLE claim ADD COLUMN claim_number INT;
