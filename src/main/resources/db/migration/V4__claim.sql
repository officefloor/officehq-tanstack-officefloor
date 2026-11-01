-- Claims a person on a sheet has made, listed newest first (by id).
CREATE TABLE claim (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet      VARCHAR(200)   NOT NULL,
    person     BIGINT         NOT NULL REFERENCES person (id),
    amount     DECIMAL(15, 2) NOT NULL,
    claimed_on DATE           NOT NULL,
    note       VARCHAR(2000)  NOT NULL
);
CREATE INDEX claim_sheet ON claim (sheet, id);
