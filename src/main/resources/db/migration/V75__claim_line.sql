-- A DRAFT claim's lines, oldest first: a label (trimmed, never empty), a quantity (a whole number, 1 or more) and a
-- unit amount (two decimals). A claim with lines has the quantities times the unit amounts, added up, as its amount.
CREATE TABLE claim_line (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    claim_id BIGINT         NOT NULL,
    label    VARCHAR(2000)  NOT NULL,
    qty      INT            NOT NULL,
    unit     DECIMAL(19, 2) NOT NULL
);
CREATE INDEX claim_line_sheet ON claim_line (sheet_id, claim_id, id);
