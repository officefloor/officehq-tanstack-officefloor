-- Every share of a claim held by another person of the sheet: the holder and their share (a percent, two decimals,
-- more than 0.00; a claim's shares add up to at most 100.00), made on the sheet's today, kept in the order made.
CREATE TABLE claim_share (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL,
    claim_id  BIGINT        NOT NULL REFERENCES claim (id),
    person_id BIGINT        NOT NULL,
    share     DECIMAL(7, 2) NOT NULL,
    made_on   DATE          NOT NULL
);
CREATE INDEX claim_share_claim ON claim_share (sheet_id, claim_id, id);
