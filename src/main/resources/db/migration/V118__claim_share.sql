-- A claim's shares, in the order added (by id): another person on the sheet holding a part of the claim's due, as a
-- percent with two decimals (above 0.00; a claim's shares add up to at most 100.00, one per person, never the claim's
-- own person). on_date is the sheet's today when the share was added (NULL when the sheet had none).
CREATE TABLE claim_share (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100)  NOT NULL REFERENCES sheet(id),
    claim_id  BIGINT        NOT NULL REFERENCES claim(id),
    person_id BIGINT        NOT NULL,
    share     DECIMAL(5, 2) NOT NULL,
    on_date   DATE
);
CREATE INDEX claim_share_sheet ON claim_share(sheet_id, claim_id, id);
