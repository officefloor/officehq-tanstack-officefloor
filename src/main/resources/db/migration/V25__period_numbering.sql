-- The sheet's period numbering from: while set and the sheet's today is on or after it, a claim
-- opened is numbered within its period (<period name>-<n>), or X-<n> when it is in no period.
-- Null (or no row) is off: claims take the sheet-wide number.
ALTER TABLE sheet_setting ADD COLUMN period_numbering_from DATE;
-- A claim's number (and the number a correction corrects) is now text: "7" sheet-wide, "P1-2" in a period.
DROP INDEX claim_sheet_number;
ALTER TABLE claim ALTER COLUMN number SET DATA TYPE VARCHAR(250);
ALTER TABLE claim ALTER COLUMN corrects SET DATA TYPE VARCHAR(250);
CREATE UNIQUE INDEX claim_sheet_number ON claim (sheet_id, number);
-- How many numbers each period sequence of a sheet has given out: `seq` is the period's name, or
-- "X" for claims in no period.
CREATE TABLE period_number (
    sheet_id VARCHAR(200) NOT NULL,
    seq      VARCHAR(200) NOT NULL,
    given    BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, seq)
);
