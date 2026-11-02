-- A sheet: everything else in the app is kept per sheet, keyed by its id. A row is made the
-- first time an id is loaded; a sheet never loaded before starts empty.
CREATE TABLE sheet (
    id         VARCHAR(200) PRIMARY KEY,
    created_by VARCHAR(200) NOT NULL
);
