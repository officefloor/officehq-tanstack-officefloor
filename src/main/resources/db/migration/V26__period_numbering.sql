-- A sheet's period numbering from-date (null means off): while it is set and the sheet's today is on
-- or after it, a claim opened is numbered within the period its date falls in, <period name>-<n>, or
-- X-<n> when in no period. Claim numbers (and the number a correction corrects) become text.
ALTER TABLE sheet ADD COLUMN period_numbering_from DATE;
ALTER TABLE claim ALTER COLUMN claim_no SET DATA TYPE VARCHAR(600);
ALTER TABLE claim ALTER COLUMN corrects SET DATA TYPE VARCHAR(600);

-- The last number given in each of a sheet's period sequences, keyed by period id ('X' for claims in
-- no period). Never reused.
CREATE TABLE claim_sequence (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    seq_key  VARCHAR(40)  NOT NULL,
    last_no  BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, seq_key)
);
