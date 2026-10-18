-- A claim opened while period numbering is on (the period-numbering-from setting is a date and the
-- sheet's today is on or after it) is numbered within its period, <period name>-<n>, or X-<n> when
-- dated in no period, instead of taking the sheet-wide number. Each sequence keeps the last number it
-- gave (never reused); the sheet-wide sequence is left as it was.
ALTER TABLE claim ADD COLUMN period_number VARCHAR(120);
CREATE UNIQUE INDEX claim_sheet_period_number ON claim (sheet_id, period_number);

CREATE TABLE claim_sequence (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(100) NOT NULL,
    seq_key     VARCHAR(120) NOT NULL,
    last_number INT          NOT NULL
);
CREATE UNIQUE INDEX claim_sequence_sheet_key ON claim_sequence (sheet_id, seq_key);
