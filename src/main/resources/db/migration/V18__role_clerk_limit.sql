-- A sheet's roles: a user name's role on the sheet, clerk or steward. A name with no row is a clerk.
CREATE TABLE sheet_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    user_name VARCHAR(200) NOT NULL,
    role VARCHAR(20) NOT NULL
);
CREATE UNIQUE INDEX sheet_role_user ON sheet_role (sheet_id, user_name);
-- The sheet's clerk limit: a clerk settles a claim only when its due is at or below it. NULL: no limit.
ALTER TABLE sheet ADD COLUMN clerk_limit DECIMAL(19, 2);
