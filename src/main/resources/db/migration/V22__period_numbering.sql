-- A claim's number is now text: a sheet-wide number (1, 2, ...) or, while the sheet numbers by period, a number
-- within its period (<period name>-<n>, or X-<n> for a claim in no period). A correction names its claim the same way.
ALTER TABLE claim ALTER COLUMN claim_number SET DATA TYPE VARCHAR(220);
ALTER TABLE claim ALTER COLUMN corrects SET DATA TYPE VARCHAR(220);

-- Each sheet's per-period sequences, by key (a period's id, or X for claims in no period): the last number given.
CREATE TABLE claim_sequence (
    sheet_id    VARCHAR(200) NOT NULL REFERENCES sheet (id),
    seq_key     VARCHAR(50)  NOT NULL,
    last_number INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (sheet_id, seq_key)
);
