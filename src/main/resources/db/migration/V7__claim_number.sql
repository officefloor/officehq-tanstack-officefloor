-- A claim's number: given when it is opened, the next on its sheet (1, 2, ...), never reused.
-- A draft has none; voiding a claim clears it. claim_number_seq holds the last number each sheet gave.
ALTER TABLE claim ADD COLUMN claim_number BIGINT;
CREATE TABLE claim_number_seq (
    sheet_id    VARCHAR(255) PRIMARY KEY,
    last_number BIGINT NOT NULL
);
