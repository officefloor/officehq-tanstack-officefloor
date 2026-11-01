-- A claim opened while the sheet numbers by period gets a number within its period instead of one on the
-- sheet: <period name>-<n>, or X-<n> for a claim in no period. Null for a claim numbered on the sheet.
ALTER TABLE claim ADD COLUMN period_number VARCHAR(220);
-- The last number given in each period of each sheet; '' is the sequence of claims in no period.
CREATE TABLE claim_period_number (
    sheet       VARCHAR(200) NOT NULL,
    period      VARCHAR(200) NOT NULL,
    last_number INT NOT NULL,
    PRIMARY KEY (sheet, period)
);
-- A correction names the claim it corrects by the number that claim shows: its number within its period, or
-- its number on the sheet.
ALTER TABLE claim ALTER COLUMN corrects SET DATA TYPE VARCHAR(220);
