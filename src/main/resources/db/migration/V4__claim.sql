-- A person's claims. The id is assigned by the store, so it orders claims as they were added.
-- The amount is kept exactly, with two decimals; the date is the claim's own (the sheet's today
-- when none was given); the note may be empty.
CREATE TABLE claim (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)   NOT NULL,
    person_id  BIGINT         NOT NULL REFERENCES person (id),
    amount     DECIMAL(19, 2) NOT NULL,
    claim_date DATE           NOT NULL,
    note       VARCHAR(2000)  NOT NULL
);
CREATE INDEX claim_sheet ON claim (sheet_id, person_id, id);
