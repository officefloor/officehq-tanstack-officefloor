-- The sheet's list of void reasons, in the order added: while it is not empty a void needs one of them.
CREATE TABLE void_reason (
    seq      BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)  NOT NULL,
    reason   VARCHAR(2000) NOT NULL,
    UNIQUE (sheet_id, reason)
);

-- The reason a claim was voided with. Null: voided without one (or not voided).
ALTER TABLE claim ADD COLUMN void_reason VARCHAR(2000);
