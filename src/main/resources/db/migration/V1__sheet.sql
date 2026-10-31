-- One row per sheet that has been loaded. `today` is the sheet's own date: every date-dependent
-- decision and every default date on the sheet uses it, never the real clock.
CREATE TABLE sheet (
    id    VARCHAR(200) PRIMARY KEY,
    today DATE         NOT NULL
);
