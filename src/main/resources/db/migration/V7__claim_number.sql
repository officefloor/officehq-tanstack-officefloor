-- A claim's number on its sheet: given when the claim is opened (1, 2, ... with no gaps, never
-- reused), NULL for a draft, and cleared when the claim is voided.
-- last_claim_number: the last number the sheet gave out; 0 before any claim was opened.
ALTER TABLE sheet ADD COLUMN last_claim_number INT DEFAULT 0 NOT NULL;
ALTER TABLE claim ADD COLUMN claim_number INT;
