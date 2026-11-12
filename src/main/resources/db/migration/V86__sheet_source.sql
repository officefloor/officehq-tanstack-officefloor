-- The sheet a sheet was cloned from (its source): a grade with no rate in force on a date on the sheet nor on
-- its parents takes the source's (then up the source's parents and source). Not dated: a clone keeps its source.
CREATE TABLE sheet_source (
    sheet_id   VARCHAR(255) PRIMARY KEY,
    source_id  VARCHAR(255) NOT NULL
);
