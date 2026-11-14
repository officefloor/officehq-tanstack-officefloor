-- The last sheet-wide number given in each fiscal year (the calendar year it starts in): a claim opened under
-- sheet-wide numbering is numbered FY<year>-<n>, the sequence shared across the sheet's tree as last_claim_number
-- was. Numbers given before stay as they are.
CREATE TABLE fiscal_claim_number (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    fiscal_year INT NOT NULL,
    last_number INT NOT NULL,
    PRIMARY KEY (sheet_id, fiscal_year)
);
