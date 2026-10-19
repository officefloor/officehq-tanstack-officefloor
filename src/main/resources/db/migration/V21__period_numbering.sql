-- A claim's number is text: the sheet-wide 1, 2, ... or, while the period-numbering-from setting is in
-- force when the claim is opened, <period name>-<n> (X-<n> for a claim in no period). A correction names
-- the claim it corrects by that same text.
ALTER TABLE claim ALTER COLUMN claim_number VARCHAR(220);
ALTER TABLE claim ALTER COLUMN corrects VARCHAR(220);
-- The last number each period sequence of a sheet gave out. seq: the period's name in lower case, prefixed
-- 'P:', or 'X' for claims in no period. A sequence without a row has given out none.
CREATE TABLE claim_number_seq (
    sheet_id VARCHAR(100) NOT NULL,
    seq      VARCHAR(210) NOT NULL,
    last_n   INT          NOT NULL,
    PRIMARY KEY (sheet_id, seq)
);
