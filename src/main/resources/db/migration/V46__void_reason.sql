-- The sheet's void reasons, each at most once, in the order added; and the reason each claim was voided with (none
-- for a claim voided while the list was empty).
CREATE TABLE void_reason (
    sheet    VARCHAR(200) NOT NULL,
    reason   VARCHAR(200) NOT NULL,
    position INT          NOT NULL,
    PRIMARY KEY (sheet, reason)
);
CREATE TABLE claim_void (
    sheet  VARCHAR(200) NOT NULL,
    claim  BIGINT       NOT NULL,
    reason VARCHAR(200) NOT NULL,
    PRIMARY KEY (sheet, claim)
);
