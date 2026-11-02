-- The tags claims carried on a sheet before leaving it (a person's transfer takes their claims away), so the sheet
-- still lists a due-total for every tag ever used on it.
CREATE TABLE sheet_tag (
    sheet VARCHAR(200)  NOT NULL,
    tag   VARCHAR(4000) NOT NULL,
    PRIMARY KEY (sheet, tag)
);
