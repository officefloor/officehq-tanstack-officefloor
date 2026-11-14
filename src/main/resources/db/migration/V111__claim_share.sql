-- The other people a claim is shared with, in the order they were given a share: each holds a percent of the claim's
-- due (two decimals, more than 0.00; a claim's shares add up to at most 100.00). A person holds at most one share of a
-- claim, and never one of their own claim.
CREATE TABLE claim_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    claim_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    share DECIMAL(5, 2) NOT NULL
);
CREATE INDEX claim_share_sheet ON claim_share (sheet_id, claim_id, id);
