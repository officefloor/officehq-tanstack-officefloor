-- A claim's shares, oldest first: another person of the sheet holding a percent (two decimals, more than 0.00) of
-- the claim's due. Each holder's due-total counts their part; the claim's own person's counts the rest.
CREATE TABLE claim_share (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    claim_id  BIGINT         NOT NULL,
    person_id BIGINT         NOT NULL,
    share     DECIMAL(19, 2) NOT NULL
);
CREATE INDEX claim_share_sheet ON claim_share (sheet_id, claim_id, id);
