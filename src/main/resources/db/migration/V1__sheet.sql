-- One row per sheet that has stored state of its own. A sheet without a row starts empty.
-- today: the sheet's "today" date; NULL means the real date.
CREATE TABLE sheet (
    id    VARCHAR(100) PRIMARY KEY,
    today DATE
);
