-- A claim's status log: one line per status it has had, oldest first (by id) — the status, who made the change,
-- the sheet's today then and the claim's due as it was worked out at that moment (null: n/a).
CREATE TABLE claim_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255) NOT NULL,
    claim_id BIGINT NOT NULL,
    status VARCHAR(10) NOT NULL,
    by_user VARCHAR(255) NOT NULL,
    on_date DATE NOT NULL,
    due DECIMAL(19, 2)
);
CREATE INDEX claim_log_sheet ON claim_log (sheet_id, claim_id);
