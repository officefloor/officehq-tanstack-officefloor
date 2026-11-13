-- A claim's tags (null: none): lower-cased, sorted and comma separated. Every tag ever given to a claim on a sheet
-- stays on the sheet's tag list, even once no claim carries it.
ALTER TABLE claim ADD COLUMN tags VARCHAR(1000);
CREATE TABLE claim_tag (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    tag VARCHAR(1000) NOT NULL,
    PRIMARY KEY (sheet_id, tag)
);
