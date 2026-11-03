-- The people-list columns each user has switched off on their screen, kept by their signed-in name as a
-- comma-separated list. A user with no row sees every column. Not a sheet's state: it has no sheet_id.
CREATE TABLE user_columns (
    user_name VARCHAR(255) PRIMARY KEY,
    hidden VARCHAR(1000) NOT NULL
);
