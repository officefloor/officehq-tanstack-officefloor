-- A sheet's claim templates: a name (unique on the sheet; saving it again replaces the template), an
-- amount (0.00 or more, two decimals) and a note. A claim made from one copies its amount and note then.
CREATE TABLE claim_template (
    sheet_id VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    name     VARCHAR(500)   NOT NULL,
    amount   DECIMAL(19, 2) NOT NULL CHECK (amount >= 0),
    note     VARCHAR(2000)  NOT NULL,
    PRIMARY KEY (sheet_id, name)
);
