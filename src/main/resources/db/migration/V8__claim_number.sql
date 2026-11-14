-- A claim's number: given when it is opened, the sheet's next (1, 2, ...), with no gaps and never
-- reused. A draft has none; voiding clears it. The sheet keeps the last number it gave.
ALTER TABLE sheet ADD COLUMN last_claim_number BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE claim ADD COLUMN number BIGINT;
