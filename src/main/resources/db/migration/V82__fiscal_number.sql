-- Sheet-wide numbering restarts each fiscal year: a claim opened takes <FY>-<n>, counting from 1
-- within the fiscal year of its due-by, the sequence shared across the sheet's tree. The last
-- number given in each fiscal year, which every sheet of the tree holds. Never given out again;
-- numbers already given stay as they are.
CREATE TABLE fiscal_number (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    fiscal_year VARCHAR(20) NOT NULL,
    last_number INT NOT NULL,
    PRIMARY KEY (sheet_id, fiscal_year)
);
