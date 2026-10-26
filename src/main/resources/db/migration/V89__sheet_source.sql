-- The sheet a cloned sheet was cloned from: its rates (then its parents', then its own source's) stand in for a grade
-- without a rate in force on a date on the clone nor on its parents.
CREATE TABLE sheet_source (
    sheet_id  VARCHAR(200) NOT NULL PRIMARY KEY REFERENCES sheet (id),
    source_id VARCHAR(200) NOT NULL
);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.sheet_source AS SELECT s.*, CAST(NULL AS BIGINT) AS snapshot_id FROM sheet_source s WHERE FALSE;
CREATE INDEX hist_sheet_source_snapshot ON hist.sheet_source (snapshot_id);
CREATE TABLE asof.sheet_source AS SELECT * FROM sheet_source WHERE FALSE;
