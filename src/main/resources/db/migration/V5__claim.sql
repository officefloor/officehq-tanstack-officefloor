-- Claims a person on a sheet has made: an amount (0.00 or more, two decimals), a date (defaulting to
-- the sheet's today) and a note (may be empty). Listed newest first (by id, descending).
CREATE TABLE claim (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(100)   NOT NULL,
    person_id  BIGINT         NOT NULL REFERENCES person (id),
    amount     DECIMAL(14, 2) NOT NULL,
    claim_date DATE           NOT NULL,
    note       VARCHAR(1000)  NOT NULL
);
CREATE INDEX claim_sheet ON claim (sheet_id, id);
