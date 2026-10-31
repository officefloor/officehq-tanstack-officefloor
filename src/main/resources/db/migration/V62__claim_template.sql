-- The sheet's claim templates: a name (trimmed, at most once per sheet), an amount and a note to add
-- a DRAFT claim with. A claim made from one copies them, so a later change does not alter it.
CREATE TABLE claim_template (
    sheet_id VARCHAR(200)  NOT NULL,
    name     VARCHAR(200)  NOT NULL,
    amount   DECIMAL(14,2) NOT NULL,
    note     VARCHAR(2000) NOT NULL,
    PRIMARY KEY (sheet_id, name)
);
