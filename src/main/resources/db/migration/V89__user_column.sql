-- The people-list columns each user has switched off on their screen. A user with no row shows
-- every column.
CREATE TABLE user_column_hidden (
    user_name VARCHAR(200) NOT NULL,
    column_key VARCHAR(40) NOT NULL,
    PRIMARY KEY (user_name, column_key)
);
