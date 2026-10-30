-- One row per sheet that has been loaded. `today` is the sheet's own date: every date decision and
-- default date in the app uses it, never the real clock. It starts as the real date on first load.
CREATE TABLE sheet (
    id    VARCHAR(200) PRIMARY KEY,
    today DATE         NOT NULL
);
