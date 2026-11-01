-- The sheet a sheet was cloned from: a grade with no rate on the sheet nor on its parents takes the
-- source's (see rate.Rates#lineage). Set once, by the clone, so it is not part of a sheet's history.
CREATE TABLE public.sheet_source (
    sheet_id  VARCHAR(200) PRIMARY KEY,
    source_id VARCHAR(200) NOT NULL
);
