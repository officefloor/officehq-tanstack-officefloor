-- A claim's number becomes text: the sheet-wide 1, 2, ... or, while the sheet numbers by period, <period>-<n>
-- (X-<n> for a claim in no period). A correction names the number it corrects the same way.
-- claim_period_number_seq holds the last number each sheet gave in each period ('' is the no-period sequence).
ALTER TABLE claim ALTER COLUMN claim_number SET DATA TYPE VARCHAR(300);
ALTER TABLE claim ALTER COLUMN corrects SET DATA TYPE VARCHAR(300);
CREATE TABLE claim_period_number_seq (
    sheet_id    VARCHAR(255) NOT NULL,
    scope       VARCHAR(255) NOT NULL,
    last_number BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, scope)
);
