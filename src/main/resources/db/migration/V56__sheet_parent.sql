-- One row per sheet id that has been loaded: such a sheet exists, so another sheet may name it as its parent.
CREATE TABLE sheet_loaded (
    id VARCHAR(200) PRIMARY KEY
);

-- Each sheet's parent, by the child's id. Not kept per sheet (no sheet column), so the sheet shown as of a date
-- keeps the parent it has now: the parent is not dated.
CREATE TABLE sheet_parent (
    child  VARCHAR(200) PRIMARY KEY,
    parent VARCHAR(200) NOT NULL
);
