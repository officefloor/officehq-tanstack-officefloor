-- A person's claims: an amount (0.00 or more, two decimals), a date and a note (may be empty).
-- Listed newest first (id order, descending).
CREATE TABLE claim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    amount DECIMAL(15, 2) NOT NULL,
    claim_date DATE NOT NULL,
    note VARCHAR(1000) NOT NULL DEFAULT ''
);
CREATE INDEX claim_sheet ON claim (sheet_id, id);
