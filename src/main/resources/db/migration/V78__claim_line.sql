-- A DRAFT claim's lines, in the order added (by id): a label (trimmed, never empty), a quantity (a whole number, at
-- least 1) and a unit amount (two decimals). A claim with lines has the quantities times the unit amounts, added up,
-- as its amount; on_date is the sheet's today when the line was added (NULL when the sheet had none).
CREATE TABLE claim_line (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100)   NOT NULL REFERENCES sheet(id),
    claim_id BIGINT         NOT NULL REFERENCES claim(id),
    label    VARCHAR(200)   NOT NULL,
    qty      INT            NOT NULL,
    unit     DECIMAL(14, 2) NOT NULL,
    on_date  DATE
);
CREATE INDEX claim_line_sheet ON claim_line(sheet_id, claim_id, id);
