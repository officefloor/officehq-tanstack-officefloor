-- The people-list columns a user name switched off on the sheet. A column with no row is shown.
CREATE TABLE user_hidden_column (
    sheet       VARCHAR(200) NOT NULL,
    user_name   VARCHAR(200) NOT NULL,
    column_name VARCHAR(50)  NOT NULL,
    PRIMARY KEY (sheet, user_name, column_name)
);
