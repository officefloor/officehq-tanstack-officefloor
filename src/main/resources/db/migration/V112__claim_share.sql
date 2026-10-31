-- A claim's holders: other people on the sheet each holding a share (more than 0.00, two decimals) of the claim's due,
-- in the order shared (by id). shared_on is the sheet's today when it was shared.
CREATE TABLE claim_share (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    claim_id  BIGINT       NOT NULL,
    person_id BIGINT       NOT NULL,
    share_pct DECIMAL(5, 2) NOT NULL,
    shared_on DATE         NOT NULL
);
CREATE INDEX claim_share_sheet ON claim_share (sheet_id, claim_id, id);
