-- The roles the sheet assigns: a user name with no row here is a clerk.
CREATE TABLE sheet_role (
    sheet_id  VARCHAR(255) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    role      VARCHAR(32)  NOT NULL,
    PRIMARY KEY (sheet_id, user_name)
);
-- The sheet's settings, by key; a null value is blank (for clerk-limit: no limit). A key with no
-- row here has never been set.
CREATE TABLE sheet_setting (
    sheet_id    VARCHAR(255) NOT NULL,
    setting_key VARCHAR(64)  NOT NULL,
    setting_value VARCHAR(255),
    PRIMARY KEY (sheet_id, setting_key)
);
