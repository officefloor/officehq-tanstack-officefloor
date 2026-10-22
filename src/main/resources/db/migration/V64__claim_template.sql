-- The sheet's claim templates: a name (trimmed, at most once on a sheet), the amount and the note a claim made from it
-- is given at that moment. Saving a name again replaces its amount and note; claims made from it keep theirs.
CREATE TABLE claim_template (
    sheet_id VARCHAR(100)   NOT NULL REFERENCES sheet(id),
    name     VARCHAR(200)   NOT NULL,
    amount   DECIMAL(14, 2) NOT NULL,
    note     VARCHAR(1000)  NOT NULL,
    saved_by VARCHAR(100)   NOT NULL,
    PRIMARY KEY (sheet_id, name)
);
