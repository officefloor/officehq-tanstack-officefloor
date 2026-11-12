-- A DRAFT claim's lines, in the order added (by id): each a label, a whole quantity (at least 1) and a unit
-- amount (two decimals), with the sheet's today when it was added. A claim with lines has the quantities
-- times the unit amounts, added up, as its amount. The as-of view shows those added on or before its date.
CREATE TABLE claim_line (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(255)   NOT NULL,
    claim_id     BIGINT         NOT NULL,
    label        VARCHAR(255)   NOT NULL,
    qty          INT            NOT NULL,
    unit         DECIMAL(19, 2) NOT NULL,
    added_on     DATE           NOT NULL
);
CREATE INDEX claim_line_sheet ON claim_line (sheet_id, claim_id, id);
