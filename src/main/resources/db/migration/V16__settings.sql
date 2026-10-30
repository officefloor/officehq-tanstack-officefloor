-- The sheet's clerk limit: a clerk may settle a claim only when its due is at or below it.
-- NULL: no limit.
ALTER TABLE sheet ADD COLUMN clerk_limit DECIMAL(12,2);

-- The role a user name holds on a sheet. A name with no row is a clerk.
CREATE TABLE sheet_role (
    sheet_id  VARCHAR(200) NOT NULL,
    user_name VARCHAR(200) NOT NULL,
    role      VARCHAR(20)  NOT NULL,
    PRIMARY KEY (sheet_id, user_name)
);
