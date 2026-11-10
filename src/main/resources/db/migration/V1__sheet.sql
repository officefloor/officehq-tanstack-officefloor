-- A sheet: everything else in the app is kept per sheet, keyed by its id. A sheet is created the
-- first time its id is loaded, empty.
CREATE TABLE sheet (
    id VARCHAR(200) PRIMARY KEY
);
