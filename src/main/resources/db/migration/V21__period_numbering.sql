-- A claim's number is text: the sheet-wide 1, 2, ... or, opened while the sheet's period-numbering-from setting is
-- on and its today is on or after it, <period name>-<n> numbered within the period containing its date (X-<n> for
-- a claim in no period). A correction names the claim it corrects by that number.
ALTER TABLE claim ALTER COLUMN claim_number VARCHAR(250);
ALTER TABLE claim ALTER COLUMN corrects VARCHAR(250);

-- The last number given in each per-period sequence of a sheet: seq_key is the period's name, or X for claims in
-- no period. Like claim_number_seq, a number is never given twice.
CREATE TABLE claim_period_number_seq (
    sheet_id    VARCHAR(200) NOT NULL,
    seq_key     VARCHAR(200) NOT NULL,
    last_number BIGINT NOT NULL,
    PRIMARY KEY (sheet_id, seq_key)
);
