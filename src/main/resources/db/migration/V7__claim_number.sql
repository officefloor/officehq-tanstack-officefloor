-- A claim's number: given when it is opened, the next on its sheet (1, 2, ...), with no gaps and
-- never reused. A DRAFT has none; voiding a claim clears it. `sheet.claim_numbers` is how many
-- numbers the sheet has given out, so the next one is claim_numbers + 1.
ALTER TABLE claim ADD COLUMN number BIGINT;
ALTER TABLE sheet ADD COLUMN claim_numbers BIGINT DEFAULT 0 NOT NULL;
UPDATE claim c SET number = (SELECT COUNT(*) FROM claim d WHERE d.sheet_id = c.sheet_id
    AND d.status IN ('OPEN', 'SETTLED') AND d.id <= c.id) WHERE status IN ('OPEN', 'SETTLED');
UPDATE sheet s SET claim_numbers = (SELECT COUNT(*) FROM claim c WHERE c.sheet_id = s.id
    AND c.status IN ('OPEN', 'SETTLED'));
CREATE UNIQUE INDEX claim_sheet_number ON claim (sheet_id, number);
