-- A claim's number: given when it is opened, the next on its sheet (1, 2, ...), never reused. The
-- sheet keeps the last number it gave; a draft has none and voiding a claim clears it.
ALTER TABLE sheet ADD COLUMN claim_numbers INT DEFAULT 0 NOT NULL;
ALTER TABLE claim ADD COLUMN claim_number INT;
CREATE UNIQUE INDEX claim_sheet_number ON claim (sheet_id, claim_number);
