-- A sheet: everything else in the app is kept per sheet, keyed by its id.
CREATE TABLE sheet (
    id        VARCHAR(100) PRIMARY KEY,
    loaded_at TIMESTAMP    NOT NULL
);
