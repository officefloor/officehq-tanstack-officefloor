-- A DRAFT claim's lines: one row per line, in the order added (by id) — its trimmed label, its quantity (a whole number,
-- at least 1), its unit amount (two decimals) and the sheet's today when it was added. A claim with lines has the
-- quantities times the unit amounts, added up, as its amount.
CREATE TABLE claim_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255) NOT NULL,
    claim_id BIGINT NOT NULL,
    label VARCHAR(2000) NOT NULL,
    qty INT NOT NULL,
    unit DECIMAL(19, 2) NOT NULL,
    on_date DATE NOT NULL
);
CREATE INDEX claim_line_sheet ON claim_line (sheet_id, claim_id);
