-- A claim's tags: trimmed, lower-cased, without repeats, sorted and comma separated (none: no tags).
ALTER TABLE claim ADD COLUMN tags VARCHAR(2000);
-- Every tag ever used on a sheet, kept when the claims carrying it no longer do.
CREATE TABLE sheet_tag (
    sheet_id VARCHAR(200) NOT NULL,
    tag      VARCHAR(200) NOT NULL,
    PRIMARY KEY (sheet_id, tag)
);
