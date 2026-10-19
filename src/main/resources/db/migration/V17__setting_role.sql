-- The sheet's settings, by key. A key without a row is blank (for clerk-limit: no limit).
CREATE TABLE sheet_setting (
    sheet_id VARCHAR(100) NOT NULL,
    setting  VARCHAR(50)  NOT NULL,
    val      VARCHAR(100) NOT NULL,
    PRIMARY KEY (sheet_id, setting)
);
-- The role the sheet assigns a user name: clerk or steward. A name without a row is a clerk.
CREATE TABLE sheet_role (
    sheet_id  VARCHAR(100) NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    role      VARCHAR(20)  NOT NULL CHECK (role IN ('clerk', 'steward')),
    PRIMARY KEY (sheet_id, user_name)
);
