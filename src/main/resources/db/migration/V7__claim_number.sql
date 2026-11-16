-- A claim's number: given when the claim is opened, the next on its sheet (1, 2, ...), with no
-- gaps and never reused. A draft has none, and voiding a claim clears it. The last number given on
-- each sheet is kept in claim_number_seq, so a cleared number is never given again.
ALTER TABLE claim ADD COLUMN claim_number BIGINT;
CREATE TABLE claim_number_seq (
    sheet_id    VARCHAR(200) PRIMARY KEY,
    last_number BIGINT NOT NULL
);
