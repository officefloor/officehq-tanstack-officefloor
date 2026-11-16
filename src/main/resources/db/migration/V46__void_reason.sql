-- The sheet's void reasons: each (trimmed, not blank) at most once, kept in the order added. While a sheet has any,
-- voiding a claim needs one of them; claim_void_reason keeps the reason each claim was voided with.
CREATE TABLE void_reason (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    reason   VARCHAR(200) NOT NULL,
    CONSTRAINT void_reason_unique UNIQUE (sheet_id, reason)
);

CREATE TABLE claim_void_reason (
    claim_id BIGINT       NOT NULL PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    reason   VARCHAR(200) NOT NULL
);
CREATE INDEX claim_void_reason_sheet ON claim_void_reason (sheet_id, reason);
