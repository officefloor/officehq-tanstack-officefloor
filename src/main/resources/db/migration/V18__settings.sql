-- The sheet's clerk limit: a clerk settles a claim only when its due is at or below it; null: no limit.
ALTER TABLE sheet ADD COLUMN clerk_limit DECIMAL(15, 2);
-- The role a user name has on a sheet; a name with no row is a clerk.
CREATE TABLE user_role (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    user_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (sheet_id, user_name)
);
