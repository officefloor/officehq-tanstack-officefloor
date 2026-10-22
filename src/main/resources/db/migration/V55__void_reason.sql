-- The sheet's void reasons: while a sheet has any, a claim is voided with one of them. A reason (trimmed) is on a
-- sheet's list at most once.
CREATE TABLE sheet_void_reason (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100)  NOT NULL REFERENCES sheet(id),
    reason   VARCHAR(1000) NOT NULL,
    added_by VARCHAR(100)  NOT NULL,
    CONSTRAINT sheet_void_reason_text UNIQUE (sheet_id, reason)
);

-- The reason a claim was voided with (none when it was voided while the sheet's list was empty).
CREATE TABLE claim_void_reason (
    sheet_id VARCHAR(100)  NOT NULL,
    claim_id BIGINT        NOT NULL,
    reason   VARCHAR(1000) NOT NULL,
    PRIMARY KEY (sheet_id, claim_id)
);
