-- The sheet a sheet was cloned from: one row per cloned sheet. A grade with no rate in force on a date on the sheet
-- nor on its parents takes the source's (then the source's parents', then its source's, and so on). A sheet with no
-- row here was not cloned.
CREATE TABLE sheet_source (
    sheet_id  VARCHAR(200) PRIMARY KEY,
    source_id VARCHAR(200) NOT NULL
);
