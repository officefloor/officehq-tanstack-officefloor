-- A claim's number is now text: the sheet-wide number (1, 2, ...) or, while the sheet's
-- period-numbering-from setting is in force when it is opened, <period name>-<n> (X-<n> for a claim
-- in no period). A correction names the number of the claim it corrects in the same form.
ALTER TABLE claim ALTER COLUMN number SET DATA TYPE VARCHAR(220);
ALTER TABLE claim ALTER COLUMN corrects SET DATA TYPE VARCHAR(220);
-- The last number given in each period's own sequence on a sheet; the key '' is the sequence of
-- claims in no period. Numbers are never reused, so the sheet keeps them past a claim's voiding.
CREATE TABLE sheet_period_number (
    sheet_id    VARCHAR(200) NOT NULL REFERENCES sheet (id),
    period_key  VARCHAR(200) NOT NULL,
    last_number BIGINT       DEFAULT 0 NOT NULL,
    PRIMARY KEY (sheet_id, period_key)
);
