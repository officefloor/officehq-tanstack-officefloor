-- A sheet is the unit everything else is kept under. A row is made the first time its id is loaded;
-- a sheet that has never been loaded has no row and starts empty.
CREATE TABLE sheet (
    id         VARCHAR(200) PRIMARY KEY,
    created_by VARCHAR(200) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
