-- The values a claim had before each edit, and the sheet's today when the edit was made, in the order made: a sheet
-- shown as of a date shows each claim with the values it had before its first edit after that date.
CREATE TABLE claim_edit (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)   NOT NULL,
    claim_id   BIGINT         NOT NULL REFERENCES claim (id),
    amount     DECIMAL(19, 2) NOT NULL,
    claim_date DATE           NOT NULL,
    note       VARCHAR(2000)  NOT NULL,
    corrects   VARCHAR(250),
    made_on    DATE           NOT NULL
);
CREATE INDEX claim_edit_sheet ON claim_edit (sheet_id, claim_id, id);
