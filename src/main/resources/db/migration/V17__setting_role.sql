-- A sheet's settings, one value per key (clerk-limit: a two-decimal amount; null: no limit). A key with no row
-- has no value.
CREATE TABLE setting (
    sheet_id    VARCHAR(200) NOT NULL,
    setting_key VARCHAR(50)  NOT NULL,
    setting_value VARCHAR(50),
    PRIMARY KEY (sheet_id, setting_key)
);

-- The role a user name has on a sheet: clerk or steward. A name with no row is a clerk.
CREATE TABLE user_role (
    sheet_id  VARCHAR(200) NOT NULL,
    user_name VARCHAR(200) NOT NULL,
    role      VARCHAR(20)  NOT NULL,
    PRIMARY KEY (sheet_id, user_name)
);
