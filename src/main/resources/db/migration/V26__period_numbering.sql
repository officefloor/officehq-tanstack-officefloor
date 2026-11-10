-- A sheet's period numbering from: while it is a date and the sheet's today is on or after it, a
-- claim opened is numbered within its period (<period name>-<n>), or X-<n> when it is in no period.
-- Null: off, claims take the sheet-wide number.
ALTER TABLE sheet ADD COLUMN period_numbering_from DATE;
-- A claim's number, and the number a correction names, are now text: a sheet-wide number (1, 2, ...)
-- or a number within a period (P1-1, X-1, ...).
ALTER TABLE claim ALTER COLUMN claim_number VARCHAR(200);
ALTER TABLE claim ALTER COLUMN corrects VARCHAR(200);
-- The last number given in each period's sequence on a sheet ('' is the sequence of claims in no
-- period). Never given out again.
CREATE TABLE period_number (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    seq_name VARCHAR(200) NOT NULL,
    last_number INT NOT NULL,
    PRIMARY KEY (sheet_id, seq_name)
);
