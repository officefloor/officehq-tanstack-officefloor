-- An opened claim gets the sheet's next number (1, 2, ...), never reused; a draft has none, and
-- voiding a claim clears it. The sheet keeps the last number it gave out.
ALTER TABLE sheet ADD COLUMN last_claim_number INT DEFAULT 0 NOT NULL;
ALTER TABLE claim ADD COLUMN claim_number INT;
