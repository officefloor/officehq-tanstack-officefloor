-- A column of the people list a signed-in user has switched off on the sheet; no row is shown.
CREATE TABLE user_column (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    user_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(40) NOT NULL,
    PRIMARY KEY (sheet_id, user_name, column_name)
);
