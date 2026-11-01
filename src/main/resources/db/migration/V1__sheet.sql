-- One row per sheet whose settings have been changed. A sheet with no row has today = the real date.
CREATE TABLE sheet (
    id    VARCHAR(200) PRIMARY KEY,
    today DATE NOT NULL
);
