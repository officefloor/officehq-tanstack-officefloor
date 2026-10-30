-- The sheet's claim templates, by name: saving a name again replaces its amount and note.
CREATE TABLE claim_template (
    sheet_id VARCHAR(200)   NOT NULL,
    name     VARCHAR(2000)  NOT NULL,
    amount   DECIMAL(19, 2) NOT NULL,
    note     VARCHAR(2000)  NOT NULL,
    PRIMARY KEY (sheet_id, name)
);
