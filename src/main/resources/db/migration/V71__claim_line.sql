-- The lines of a claim, kept in the order added: a label, a quantity (a whole number, at least 1) and a unit amount
-- (two decimals); with any, the claim's amount is the quantities times the unit amounts added up. Who added each and
-- the sheet's today then are kept with it.
CREATE TABLE claim_line (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)   NOT NULL,
    claim_id BIGINT         NOT NULL REFERENCES claim (id),
    added_by VARCHAR(200)   NOT NULL,
    added_on DATE           NOT NULL,
    label    VARCHAR(2000)  NOT NULL,
    qty      INT            NOT NULL,
    unit     DECIMAL(19, 2) NOT NULL
);
CREATE INDEX claim_line_claim ON claim_line (sheet_id, claim_id, id);
