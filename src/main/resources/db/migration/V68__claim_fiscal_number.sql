-- Sheet-wide numbering restarts each fiscal year: a claim opened is numbered FY<yyyy>-<n>, n counting from 1 within
-- the fiscal year its due-by falls in, shared across the sheet's tree. claim_fiscal_number_seq holds the last number
-- each sheet gave in each fiscal year (FY<yyyy>). Numbers given before stay as they are.
CREATE TABLE claim_fiscal_number_seq (
    sheet_id    VARCHAR(255) NOT NULL,
    fiscal_year VARCHAR(16)  NOT NULL,
    last_number BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, fiscal_year)
);
