-- Period numbering: from period_numbering_from (NULL: off), a claim opened on or after it is numbered
-- within its period (<period>-<n>, or X-<n> for a claim in no period). A claim's number, and the
-- number a correction names, are therefore text; earlier claims keep their sheet-wide numbers.
ALTER TABLE sheet ADD COLUMN period_numbering_from DATE;
ALTER TABLE claim ALTER COLUMN claim_number SET DATA TYPE VARCHAR(220);
ALTER TABLE claim ALTER COLUMN corrects SET DATA TYPE VARCHAR(220);
-- The last number given in each period's sequence; period_name '' is the sequence of claims in no period.
CREATE TABLE period_claim_number (
    sheet_id    VARCHAR(200) NOT NULL,
    period_name VARCHAR(200) NOT NULL,
    last_number BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, period_name)
);
