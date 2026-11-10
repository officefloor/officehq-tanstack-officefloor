-- A person's claims, listed newest first (by id). An amount is 0.00 or more, with up to two
-- decimals; a date defaults to the sheet's today when the claim is made; a note may be empty.
CREATE TABLE claim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    amount DECIMAL(19, 2) NOT NULL,
    claim_date DATE NOT NULL,
    note VARCHAR(1000) NOT NULL DEFAULT ''
);
CREATE INDEX claim_sheet ON claim(sheet_id, id);
