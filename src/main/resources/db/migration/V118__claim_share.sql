-- A claim's shares with other people on the sheet, oldest first (by id): each holder (a person other
-- than the claim's own) at a percent with two decimals, more than 0.00, the claim's shares together at
-- most 100.00. A holder's part of the claim's due is its due times their share divided by 100, rounded
-- half to even; the claim's own person keeps the rest.
CREATE TABLE claim_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    claim_id BIGINT NOT NULL REFERENCES claim(id),
    person_id BIGINT NOT NULL,
    share DECIMAL(5, 2) NOT NULL,
    UNIQUE (sheet_id, claim_id, person_id)
);
CREATE INDEX claim_share_sheet ON claim_share(sheet_id, claim_id, id);
