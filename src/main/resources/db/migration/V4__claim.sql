-- A person's claims: an amount (0.00 or more, two decimals), a date and a note (may be empty).
-- Listed newest first (by id).
CREATE TABLE claim (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(255)   NOT NULL,
    person_id  BIGINT         NOT NULL,
    amount     DECIMAL(19, 2) NOT NULL,
    claim_date DATE           NOT NULL,
    note       VARCHAR(4000)  NOT NULL
);
CREATE INDEX claim_sheet ON claim (sheet_id, person_id, id);
