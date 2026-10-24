-- A claim by a person on a sheet: an amount (0.00 or more, two decimals), a date and a note (may be
-- empty). Listed under its person newest first (by id, descending).
CREATE TABLE claim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    amount DECIMAL(19, 2) NOT NULL,
    claim_date DATE NOT NULL,
    note VARCHAR(2000) NOT NULL DEFAULT ''
);
CREATE INDEX claim_sheet ON claim (sheet_id, id);
