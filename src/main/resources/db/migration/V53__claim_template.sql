-- The sheet's claim templates, one per name (saving a name again replaces it), in the order first saved. A claim made
-- from one copies its amount and note, so a later change to the template leaves the claim as it is.
CREATE TABLE claim_template (
    sheet    VARCHAR(200)   NOT NULL,
    name     VARCHAR(200)   NOT NULL,
    amount   DECIMAL(12, 2) NOT NULL,
    note     VARCHAR(1000)  NOT NULL,
    position INT            NOT NULL,
    PRIMARY KEY (sheet, name)
);
