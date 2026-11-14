-- A person's claims: an amount (0.00 or more, two decimals), a date and a note (may be empty).
-- Listed newest first (by id).
CREATE TABLE claim (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    person_id  BIGINT         NOT NULL REFERENCES person (id),
    amount     DECIMAL(19, 2) NOT NULL CHECK (amount >= 0),
    claim_date DATE           NOT NULL,
    note       VARCHAR(2000)  NOT NULL DEFAULT '',
    created_by VARCHAR(200)   NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX claim_sheet ON claim (sheet_id, id);
