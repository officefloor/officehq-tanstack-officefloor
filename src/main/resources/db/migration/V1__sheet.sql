-- A sheet: everything else in the app is kept per sheet. A row is created the first time its id
-- is loaded, so a sheet that was never loaded before starts empty.
CREATE TABLE sheet (
    id         VARCHAR(200) PRIMARY KEY,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
