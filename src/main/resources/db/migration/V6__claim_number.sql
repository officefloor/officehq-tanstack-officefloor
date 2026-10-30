-- A claim's number: assigned when it is opened, the next on its sheet (1, 2, ... with no gaps, never
-- reused). A draft has none; voiding a claim clears it. last_claim_number is the sheet's counter.
ALTER TABLE claim ADD COLUMN claim_number BIGINT;
ALTER TABLE sheet ADD COLUMN last_claim_number BIGINT DEFAULT 0 NOT NULL;
