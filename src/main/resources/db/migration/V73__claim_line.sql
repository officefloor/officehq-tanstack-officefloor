-- A DRAFT claim's lines: one row per line, oldest first by id. With lines, the claim's amount is the sum of qty x unit.
CREATE TABLE claim_line (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)   NOT NULL,
    claim_id BIGINT         NOT NULL,
    label    VARCHAR(4000)  NOT NULL,
    qty      INT            NOT NULL,
    unit     DECIMAL(19, 2) NOT NULL
);
CREATE INDEX claim_line_sheet ON claim_line (sheet_id, claim_id, id);
