-- The reason a VOID claim was voided with, chosen from its sheet's void reasons (null: none given).
ALTER TABLE claim ADD COLUMN void_reason VARCHAR(600);

-- The sheet's void reasons, trimmed, in the order they were added: while there is one, voiding a claim needs
-- one of them. Each reason is on the sheet's list at most once.
CREATE TABLE void_reason (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    position INT          NOT NULL,
    reason   VARCHAR(600) NOT NULL,
    PRIMARY KEY (sheet_id, reason)
);
