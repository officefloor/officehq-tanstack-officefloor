-- The sheet's period numbering from (null: off). While it is a date and the sheet's today is on or after it, a
-- claim opened is numbered within its period: <period name>-<n>, counting from 1 in each period; a claim in no
-- period is numbered X-<n> in its own sequence. A claim's number (and the number a correction names) is now text:
-- a sheet-wide number is kept as its digits.
ALTER TABLE sheet ADD COLUMN period_numbering_from DATE;
ALTER TABLE claim ALTER COLUMN claim_number SET DATA TYPE VARCHAR(220);
ALTER TABLE claim ALTER COLUMN corrects SET DATA TYPE VARCHAR(220);
-- The last number given in each period's sequence on a sheet ('X' for the claims in no period), by the
-- period's name as compared on the sheet (lower case, trimmed).
CREATE TABLE period_number (
    sheet_id    VARCHAR(100) NOT NULL REFERENCES sheet(id),
    seq_key     VARCHAR(220) NOT NULL,
    last_number INT          NOT NULL,
    PRIMARY KEY (sheet_id, seq_key)
);
