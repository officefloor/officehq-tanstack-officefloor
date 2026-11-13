-- The sheet's claim templates: a name (trimmed, unique on the sheet), an amount (0.00 or more, two decimals) and a
-- note (may be empty). Saving a name again replaces its template; a claim made from one copies its amount and note.
CREATE TABLE claim_template (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    name VARCHAR(200) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    note VARCHAR(1000) NOT NULL DEFAULT '',
    PRIMARY KEY (sheet_id, name)
);
