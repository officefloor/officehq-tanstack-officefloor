-- A person's claims on a sheet, listed newest first (by id). The amount is 0.00 or more, in two
-- decimals; claim_date defaults to the sheet's today when the claim is made.
CREATE TABLE claim (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(255)   NOT NULL,
    person_id  BIGINT         NOT NULL REFERENCES person (id),
    amount     DECIMAL(15, 2) NOT NULL CHECK (amount >= 0),
    claim_date DATE           NOT NULL,
    note       VARCHAR(4000)  NOT NULL
);
CREATE INDEX claim_sheet ON claim (sheet_id, id);
