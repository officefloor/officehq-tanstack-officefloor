-- A claim's tags: trimmed, lower-cased, without repeats, sorted and comma separated (null: none).
ALTER TABLE claim ADD COLUMN tags VARCHAR(2000);

-- Every tag ever used on a sheet (a tag stays here when the claims carrying it change).
CREATE TABLE sheet_tag (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    tag      VARCHAR(600) NOT NULL,
    PRIMARY KEY (sheet_id, tag)
);
