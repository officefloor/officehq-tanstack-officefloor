-- Sheet-wide numbering restarts each fiscal year: a claim opened sheet-wide is numbered <FY>-<n>, n counting
-- from 1 within the fiscal year (FY<yyyy>, by the sheet's fiscal start month) of its period date, the sequence
-- shared across the sheet's tree. The last number given in each fiscal year's sequence, per sheet; the tree's
-- next number is one past the highest any of its sheets holds. Numbers given before stay as they were.
CREATE TABLE fiscal_claim_number (
    sheet_id    VARCHAR(200) NOT NULL,
    fiscal_year VARCHAR(20)  NOT NULL,
    last_number BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, fiscal_year)
);
