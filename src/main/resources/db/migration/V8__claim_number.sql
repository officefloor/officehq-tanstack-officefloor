-- A claim's number: given when it is opened, the next on its sheet (1, 2, ...), never reused.
-- A draft has none; voiding a claim clears it. sheet.claim_number is the last number given.
ALTER TABLE sheet ADD COLUMN claim_number BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE claim ADD COLUMN claim_no BIGINT;
