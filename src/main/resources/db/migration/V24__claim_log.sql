-- A claim's status log: one line per status it has had, in the order they happened. Each line keeps who
-- made the change, the sheet's today then, and the claim's due as it was worked out at that moment
-- (as text with two decimals, or n/a). Claims made before the log existed have no lines.
CREATE TABLE claim_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255) NOT NULL,
    claim_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    by_user VARCHAR(255),
    log_date DATE NOT NULL,
    due VARCHAR(32) NOT NULL
);
CREATE INDEX claim_log_sheet ON claim_log (sheet_id, claim_id, id);
