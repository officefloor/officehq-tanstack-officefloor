-- A claim's number: given when it is opened, the next on its sheet (1, 2, ...), never reused.
-- A draft has none, and voiding a claim clears it.
ALTER TABLE claim ADD COLUMN number BIGINT;
-- The last number given on each sheet. Its row is locked while a number is taken, so two claims
-- opened at once get different numbers.
CREATE TABLE claim_number (
    sheet_id    VARCHAR(255) PRIMARY KEY,
    last_number BIGINT NOT NULL
);
