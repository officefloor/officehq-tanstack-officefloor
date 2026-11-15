-- The columns of the people list each user has switched off on a sheet, for their own screen only.
-- A column with no row is shown.
CREATE TABLE user_hidden_column (
    sheet_id    VARCHAR(200) NOT NULL,
    user_name   VARCHAR(200) NOT NULL,
    column_name VARCHAR(50)  NOT NULL,
    PRIMARY KEY (sheet_id, user_name, column_name)
);
