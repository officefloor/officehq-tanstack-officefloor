-- The people-list columns a user has switched off on a sheet: one row per hidden column. How that user's screen shows
-- the list only; a user without rows sees every column.
CREATE TABLE user_hidden_column (
    sheet_id    VARCHAR(200) NOT NULL,
    user_name   VARCHAR(200) NOT NULL,
    column_name VARCHAR(40)  NOT NULL,
    CONSTRAINT user_hidden_column_pk PRIMARY KEY (sheet_id, user_name, column_name)
);
