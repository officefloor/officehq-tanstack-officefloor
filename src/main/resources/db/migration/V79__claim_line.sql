-- A draft claim's lines, oldest first (by id): each with its trimmed label, a whole quantity of at
-- least 1 and a unit amount with two decimals; while a claim has lines its amount is their total.
-- The sheet's today when the line was added, so the sheet read as of a date shows the lines it had.
CREATE TABLE claim_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    claim_id BIGINT NOT NULL REFERENCES claim(id),
    label VARCHAR(200) NOT NULL,
    qty INT NOT NULL,
    unit DECIMAL(17, 2) NOT NULL,
    on_date DATE NOT NULL
);
CREATE INDEX claim_line_sheet ON claim_line(sheet_id, claim_id, id);
