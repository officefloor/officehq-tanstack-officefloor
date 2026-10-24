-- A claim's number: given when it is opened, the sheet's next one (1, 2, ...), never reused. A draft
-- has none; voiding clears it. The sheet keeps the last number it gave out.
ALTER TABLE sheet ADD COLUMN last_claim_number INT DEFAULT 0 NOT NULL;
ALTER TABLE claim ADD COLUMN claim_number INT;
