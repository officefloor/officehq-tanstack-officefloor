-- The sheet's period numbering from date: while it is set and the sheet's today is on or after it, an opened
-- claim is numbered within its period (<period name>-<n>; X-<n> for a claim in no period); null: off.
ALTER TABLE sheet ADD COLUMN period_numbering_from DATE;
-- A claim's number (and the number a correction names) is now text: a sheet-wide 1, 2, ... or a per-period P1-1, X-1.
ALTER TABLE claim ALTER COLUMN claim_number SET DATA TYPE VARCHAR(120);
ALTER TABLE claim ALTER COLUMN corrects SET DATA TYPE VARCHAR(120);
-- The last number each per-period sequence gave out: seq_key is the period's name in lower case, or '' for
-- the claims in no period (X). A number taken is never given out again.
CREATE TABLE period_claim_number (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    seq_key VARCHAR(100) NOT NULL,
    last_number INT NOT NULL,
    PRIMARY KEY (sheet_id, seq_key)
);
