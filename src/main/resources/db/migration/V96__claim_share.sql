-- A claim's shares: another person on the sheet holding a percent (two decimals, more than 0.00) of its
-- due; a claim's shares add up to at most 100.00, one per holder. Listed in the order they were added (by id).
CREATE TABLE claim_share (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    claim_id  BIGINT        NOT NULL REFERENCES claim (id),
    person_id BIGINT        NOT NULL REFERENCES person (id),
    share     DECIMAL(5, 2) NOT NULL
);
CREATE INDEX claim_share_sheet ON claim_share (sheet_id, claim_id, id);
