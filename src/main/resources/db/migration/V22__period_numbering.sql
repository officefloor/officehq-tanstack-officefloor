-- A claim's number is now text: the sheet-wide 1, 2, ... as before, or, while the sheet numbers by
-- period, <period name>-<n> (X-<n> for a claim in no period). A correction names it the same way.
ALTER TABLE claim ALTER COLUMN number VARCHAR(255);
ALTER TABLE claim ALTER COLUMN corrects VARCHAR(255);
-- The last number given in each per-period sequence on a sheet ('P:<period name>', or 'X' for claims
-- in no period). Locked while a number is taken, as claim_number is.
CREATE TABLE claim_period_number (
    sheet_id    VARCHAR(255) NOT NULL,
    sequence    VARCHAR(255) NOT NULL,
    last_number BIGINT NOT NULL,
    PRIMARY KEY (sheet_id, sequence)
);
