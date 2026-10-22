-- A claim may carry tags (null: none): trimmed, lower-cased, without repeats, sorted and joined by commas.
ALTER TABLE claim ADD COLUMN tags VARCHAR(1000);
ALTER TABLE claim_history ADD COLUMN tags VARCHAR(1000);

-- Every tag ever used on a sheet's claims: kept even once no claim carries it.
CREATE TABLE claim_tag (
    sheet_id VARCHAR(100) NOT NULL,
    tag VARCHAR(1000) NOT NULL,
    PRIMARY KEY (sheet_id, tag)
);
