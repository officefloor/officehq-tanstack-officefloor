-- The sheet a clone was copied from (none: the sheet was not cloned). A grade with no rate in force on
-- a date on a clone nor on its parents takes the source's (and so on up the source's parents and source).
CREATE TABLE sheet_source (
    sheet_id  VARCHAR(200) PRIMARY KEY,
    source_id VARCHAR(200) NOT NULL
);
