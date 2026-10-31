-- A sheet's settings. A user name's role on a sheet: CLERK (the default for a name with no row)
-- or STEWARD. Setting a name's role again replaces it.
CREATE TABLE sheet_role (
    sheet_id  VARCHAR(200) NOT NULL,
    user_name VARCHAR(200) NOT NULL,
    role      VARCHAR(20)  NOT NULL,
    PRIMARY KEY (sheet_id, user_name)
);
-- The sheet's clerk limit: a clerk can settle a claim only when its due is at or below it. Null
-- (or no row) is no limit.
CREATE TABLE sheet_setting (
    sheet_id    VARCHAR(200) NOT NULL PRIMARY KEY,
    clerk_limit DECIMAL(12,2)
);
