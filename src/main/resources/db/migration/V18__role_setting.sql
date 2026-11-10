-- A user's role on a sheet: clerk or steward. A name with no row is a clerk.
CREATE TABLE sheet_role (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    user_name VARCHAR(200) NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (sheet_id, user_name)
);
-- A sheet's clerk limit: the highest due a clerk may settle, with two decimals. Null: no limit.
ALTER TABLE sheet ADD COLUMN clerk_limit DECIMAL(19, 2);
