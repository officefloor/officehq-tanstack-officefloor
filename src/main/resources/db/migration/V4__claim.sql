-- A person's claims, listed newest first (by id). `amount` is 0.00 or more, two decimals;
-- `claim_date` defaults to the sheet's today when the claim is made; `note` may be empty.
CREATE TABLE claim (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)  NOT NULL,
    person_id  BIGINT        NOT NULL REFERENCES person (id),
    amount     DECIMAL(14,2) NOT NULL CHECK (amount >= 0),
    claim_date DATE          NOT NULL,
    note       VARCHAR(2000) NOT NULL
);
CREATE INDEX claim_sheet ON claim (sheet_id, id);
