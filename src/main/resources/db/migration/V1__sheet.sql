-- A sheet's own settings. A sheet with no row here has never been changed: its today is the real
-- date. Everything else kept per sheet is keyed by sheet_id.
CREATE TABLE sheet (
    id    VARCHAR(200) PRIMARY KEY,
    today DATE NOT NULL
);
