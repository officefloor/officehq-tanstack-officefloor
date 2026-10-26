-- A sheet's own settings. A sheet with no row here has never been changed: its today is the real date.
CREATE TABLE sheet (
    id         VARCHAR(255) PRIMARY KEY,
    today_date DATE NOT NULL
);
