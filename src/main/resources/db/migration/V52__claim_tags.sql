-- A claim's tags: trimmed, lower-cased, repeats dropped, sorted and comma separated. Null: no tags.
ALTER TABLE claim ADD COLUMN tags VARCHAR(2000);

-- Every tag ever used on a sheet's claims: the sheet shows a due-total for each.
CREATE TABLE sheet_tag (
    sheet_id VARCHAR(200)  NOT NULL,
    tag      VARCHAR(2000) NOT NULL,
    PRIMARY KEY (sheet_id, tag)
);
