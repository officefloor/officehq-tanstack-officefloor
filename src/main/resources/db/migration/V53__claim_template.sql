-- The sheet's claim templates: a name (trimmed, at most once per sheet), an amount and a note. Saving a name again
-- replaces its amount and note. A claim made from a template copies them, so a later save does not alter it.
CREATE TABLE claim_template (
    sheet_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    listed BIGINT NOT NULL,
    PRIMARY KEY (sheet_id, name)
);
