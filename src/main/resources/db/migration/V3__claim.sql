-- A person's claims. Listed newest first (id is assigned in the order they were added).
-- amount is 0.00 or more with at most two decimals; claim_date defaults to the sheet's today.
CREATE TABLE claim (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)  NOT NULL,
    person_id  BIGINT        NOT NULL,
    amount     DECIMAL(14,2) NOT NULL,
    claim_date DATE          NOT NULL,
    note       VARCHAR(2000) NOT NULL
);
CREATE INDEX claim_sheet ON claim (sheet_id, person_id, id);
