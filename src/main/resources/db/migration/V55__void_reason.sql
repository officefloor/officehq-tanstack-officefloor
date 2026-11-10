-- A sheet's void reasons: trimmed text, at most once per sheet, listed in the order they were added.
-- While a sheet has any, a claim is voided only with one of them, kept on the claim (and its history).
CREATE TABLE sheet_void_reason (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    reason VARCHAR(200) NOT NULL,
    UNIQUE (sheet_id, reason)
);
ALTER TABLE claim ADD COLUMN void_reason VARCHAR(200);
ALTER TABLE claim_history ADD COLUMN void_reason VARCHAR(200);
