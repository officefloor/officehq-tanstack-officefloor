-- A draft claim's lines, in the order added (by id): a label, a whole quantity (at least 1) and a unit amount. A claim
-- with lines has its amount kept as the quantities times the unit amounts added up.
CREATE TABLE claim_line (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet   VARCHAR(200)   NOT NULL,
    claim   BIGINT         NOT NULL REFERENCES claim (id),
    label   VARCHAR(4000)  NOT NULL,
    qty     INT            NOT NULL,
    unit    DECIMAL(19, 2) NOT NULL
);
CREATE INDEX claim_line_sheet ON claim_line (sheet, claim, id);
