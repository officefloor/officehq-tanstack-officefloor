-- The sheet's period numbering from: while it is set and the sheet's today is on or after it, a claim opened is
-- numbered within its period (<period name>-<n>; X-<n> for a claim in no period). Null (blank) means off.
ALTER TABLE sheet ADD COLUMN period_numbering_from DATE;
-- A claim's number is now text: the sheet-wide 1, 2, ... or a per-period P1-1, X-1. A correction names it the same way.
ALTER TABLE claim ALTER COLUMN claim_number VARCHAR(255);
ALTER TABLE claim ALTER COLUMN corrects VARCHAR(255);
-- The last number given out in each per-period sequence of a sheet: seq is 'P:<period name>' or 'X' (no period).
CREATE TABLE claim_sequence (
    sheet_id VARCHAR(255) NOT NULL,
    seq VARCHAR(255) NOT NULL,
    last_number INT NOT NULL,
    PRIMARY KEY (sheet_id, seq)
);
