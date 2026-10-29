-- The sheet each cloned sheet was cloned from: its source, whose rates it uses when it and its parents have none in
-- force for a grade on a date. Not dated, like the parent.
CREATE TABLE sheet_source (
    id        VARCHAR(255) PRIMARY KEY,
    source_id VARCHAR(255) NOT NULL
);
